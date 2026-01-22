#!/usr/bin/env python3
"""
Test script for KillAura autoconfig algorithm

This script:
1. Calculates optimal settings from log samples (same as autoconfig)
2. Tests ALL rotation modes (Linear, Sigmoid, Interpolation, Acceleration)
3. Simulates applying those settings to the log positions
4. Calculates how well the settings match the actual player behavior
5. Optimizes settings to maximize accuracy with preference for high midpoints (>0.75)
"""

import json
import math
import random
from pathlib import Path
from typing import List, Dict, Tuple, Optional
from dataclasses import dataclass
import itertools
from abc import ABC, abstractmethod


@dataclass
class Vec2:
    x: float
    y: float


@dataclass
class Vec3:
    x: float
    y: float
    z: float


@dataclass
class CombatSample:
    current_vector: Vec3
    target_vector: Vec3
    velocity_delta: Vec2

    @property
    def total_delta_yaw(self) -> float:
        """Calculate yaw delta from current to target rotation vectors"""
        # Convert from direction vectors to rotations, then get delta
        current_yaw = math.degrees(math.atan2(self.current_vector.x, self.current_vector.z))
        target_yaw = math.degrees(math.atan2(self.target_vector.x, self.target_vector.z))

        diff = target_yaw - current_yaw
        # Normalize to -180..180
        while diff > 180:
            diff -= 360
        while diff < -180:
            diff += 360
        return diff

    @property
    def total_delta_pitch(self) -> float:
        """Calculate pitch delta from current to target rotation vectors"""
        import math
        current_pitch = math.degrees(math.asin(-self.current_vector.y))
        target_pitch = math.degrees(math.asin(-self.target_vector.y))
        return target_pitch - current_pitch


@dataclass
class KillAuraConfigSample:
    combat_data: CombatSample
    raycast_hit: bool


class InterpolationAnalyzer:
    """Analyzes combat samples and calculates optimal Interpolation settings"""

    MIN_REMAINING = 8.0    # Exclude very small adjustments
    MAX_REMAINING = 90.0
    MIN_MOVED = 1.0        # Exclude tiny movements

    def analyze(self, samples: List[KillAuraConfigSample]) -> Dict[str, any]:
        """
        Calculate optimal settings from samples
        Returns dict with horizontalSpeed, verticalSpeed, directionChangeFactor, midpoint
        """
        if not samples:
            return {}

        # Filter samples where cursor is NOT on enemy (raycastHit = false)
        yaw_percentages = []
        pitch_percentages = []

        filtered_raycast = 0
        filtered_range = 0
        filtered_moved = 0

        for sample in samples:
            if sample.raycast_hit:
                filtered_raycast += 1
                continue

            remaining_yaw = abs(sample.combat_data.total_delta_yaw)
            moved_yaw = abs(sample.combat_data.velocity_delta.x)

            if not (self.MIN_REMAINING <= remaining_yaw <= self.MAX_REMAINING):
                filtered_range += 1
            elif moved_yaw <= self.MIN_MOVED:
                filtered_moved += 1
            else:
                yaw_percentages.append((moved_yaw / remaining_yaw) * 100.0)

            remaining_pitch = abs(sample.combat_data.total_delta_pitch)
            moved_pitch = abs(sample.combat_data.velocity_delta.y)

            if self.MIN_REMAINING <= remaining_pitch <= self.MAX_REMAINING and moved_pitch > self.MIN_MOVED:
                pitch_percentages.append((moved_pitch / remaining_pitch) * 100.0)

        print(f"Filtered: {filtered_raycast} raycast_hit, {filtered_range} out of range, {filtered_moved} no movement")
        print(f"Valid: {len(yaw_percentages)} yaw samples, {len(pitch_percentages)} pitch samples")

        if not yaw_percentages and not pitch_percentages:
            return {}

        # Use P80 (80th percentile) for speed settings
        sorted_yaw = sorted(yaw_percentages)
        sorted_pitch = sorted(pitch_percentages)

        yaw_p80 = sorted_yaw[int(len(sorted_yaw) * 0.80)] if sorted_yaw else 35.0
        pitch_p80 = sorted_pitch[int(len(sorted_pitch) * 0.80)] if sorted_pitch else 25.0

        # Calculate standard deviation
        def std_dev(values: List[float]) -> float:
            if len(values) <= 1:
                return 5.0
            mean = sum(values) / len(values)
            variance = sum((x - mean) ** 2 for x in values) / len(values)
            return math.sqrt(variance)

        yaw_std = std_dev(yaw_percentages)
        pitch_std = std_dev(pitch_percentages)

        # Create narrow range centered on P80
        max_variation = 5.0
        yaw_variation = min(yaw_std * 0.25, max_variation)
        pitch_variation = min(pitch_std * 0.25, max_variation)

        h_start = max(1, int(yaw_p80 - yaw_variation))
        h_end = min(100, max(h_start + 3, int(yaw_p80 + yaw_variation)))

        v_start = max(1, int(pitch_p80 - pitch_variation))
        v_end = min(100, max(v_start + 3, int(pitch_p80 + pitch_variation)))

        # Direction change factor
        combined_std = (yaw_std + pitch_std) / 2.0
        variance_normalized = min(combined_std / 30.0, 1.0)
        dc_start = max(75, int(95 - (variance_normalized * 20)))
        dc_end = min(100, dc_start + 5)

        # Midpoint calculation
        avg_p80 = (yaw_p80 + pitch_p80) / 2.0
        if avg_p80 > 50:
            midpoint = 0.45
        elif avg_p80 > 30:
            midpoint = 0.35
        elif avg_p80 > 15:
            midpoint = 0.28
        else:
            midpoint = 0.22

        return {
            'horizontal_speed': (h_start, h_end),
            'vertical_speed': (v_start, v_end),
            'direction_change_factor': (dc_start, dc_end),
            'midpoint': midpoint,
            'yaw_p80': yaw_p80,
            'pitch_p80': pitch_p80,
            'yaw_std': yaw_std,
            'pitch_std': pitch_std,
        }


class RotationSimulator(ABC):
    """Base class for rotation mode simulators"""

    @abstractmethod
    def simulate_rotation(self, current_yaw: float, current_pitch: float,
                         target_yaw: float, target_pitch: float) -> Tuple[float, float]:
        pass

    @abstractmethod
    def get_mode_name(self) -> str:
        pass


class LinearSimulator(RotationSimulator):
    """Linear angle smooth - constant speed"""

    def __init__(self, h_speed_range: Tuple[float, float], v_speed_range: Tuple[float, float]):
        self.h_speed_range = h_speed_range
        self.v_speed_range = v_speed_range

    def simulate_rotation(self, current_yaw: float, current_pitch: float,
                         target_yaw: float, target_pitch: float) -> Tuple[float, float]:
        h_speed = random.uniform(*self.h_speed_range)
        v_speed = random.uniform(*self.v_speed_range)

        yaw_diff = target_yaw - current_yaw
        pitch_diff = target_pitch - current_pitch

        # Normalize yaw
        while yaw_diff > 180:
            yaw_diff -= 360
        while yaw_diff < -180:
            yaw_diff += 360

        # Apply linear speed
        new_yaw = current_yaw + max(-h_speed, min(h_speed, yaw_diff))
        new_pitch = current_pitch + max(-v_speed, min(v_speed, pitch_diff))
        new_pitch = max(-90, min(90, new_pitch))

        return new_yaw, new_pitch

    def get_mode_name(self) -> str:
        return "Linear"


class SigmoidSimulator(RotationSimulator):
    """Sigmoid angle smooth"""

    def __init__(self, h_speed_range: Tuple[float, float], v_speed_range: Tuple[float, float],
                 steepness: float = 10.0, midpoint: float = 0.3):
        self.h_speed_range = h_speed_range
        self.v_speed_range = v_speed_range
        self.steepness = steepness
        self.midpoint = midpoint

    def compute_factor(self, rotation_diff: float, turn_speed: float) -> float:
        scaled_diff = rotation_diff / 120.0
        sigmoid = 1.0 / (1.0 + math.exp(-self.steepness * (scaled_diff - self.midpoint)))
        return sigmoid * turn_speed

    def simulate_rotation(self, current_yaw: float, current_pitch: float,
                         target_yaw: float, target_pitch: float) -> Tuple[float, float]:
        h_speed = random.uniform(*self.h_speed_range)
        v_speed = random.uniform(*self.v_speed_range)

        yaw_diff = target_yaw - current_yaw
        pitch_diff = target_pitch - current_pitch

        while yaw_diff > 180:
            yaw_diff -= 360
        while yaw_diff < -180:
            yaw_diff += 360

        rotation_diff = math.sqrt(yaw_diff**2 + pitch_diff**2)

        h_factor = self.compute_factor(rotation_diff, h_speed)
        v_factor = self.compute_factor(rotation_diff, v_speed)

        new_yaw = current_yaw + (h_factor if yaw_diff > 0 else -h_factor)
        new_pitch = current_pitch + (v_factor if pitch_diff > 0 else -v_factor)
        new_pitch = max(-90, min(90, new_pitch))

        return new_yaw, new_pitch

    def get_mode_name(self) -> str:
        return "Sigmoid"


class AccelerationSimulator(RotationSimulator):
    """Acceleration angle smooth - physics-based acceleration"""

    def __init__(self, yaw_accel_range: Tuple[float, float], pitch_accel_range: Tuple[float, float]):
        self.yaw_accel_range = yaw_accel_range
        self.pitch_accel_range = pitch_accel_range
        self.prev_yaw_diff = 0.0
        self.prev_pitch_diff = 0.0

    def simulate_rotation(self, current_yaw: float, current_pitch: float,
                         target_yaw: float, target_pitch: float) -> Tuple[float, float]:
        yaw_diff = target_yaw - current_yaw
        pitch_diff = target_pitch - current_pitch

        # Normalize yaw
        while yaw_diff > 180:
            yaw_diff -= 360
        while yaw_diff < -180:
            yaw_diff += 360

        # Calculate acceleration needed
        yaw_accel_needed = yaw_diff - self.prev_yaw_diff
        pitch_accel_needed = pitch_diff - self.prev_pitch_diff

        # Get random acceleration from range
        yaw_accel = random.uniform(*self.yaw_accel_range)
        pitch_accel = random.uniform(*self.pitch_accel_range)

        # Clamp acceleration to needed amount
        yaw_accel = max(-yaw_accel, min(yaw_accel, yaw_accel_needed))
        pitch_accel = max(-pitch_accel, min(pitch_accel, pitch_accel_needed))

        # Apply acceleration
        new_yaw_diff = self.prev_yaw_diff + yaw_accel
        new_pitch_diff = self.prev_pitch_diff + pitch_accel

        new_yaw = current_yaw + new_yaw_diff
        new_pitch = current_pitch + new_pitch_diff
        new_pitch = max(-90, min(90, new_pitch))

        # Store for next iteration
        self.prev_yaw_diff = new_yaw_diff
        self.prev_pitch_diff = new_pitch_diff

        return new_yaw, new_pitch

    def get_mode_name(self) -> str:
        return "Acceleration"


class InterpolationSimulator(RotationSimulator):
    """Simulates the Interpolation angle smooth algorithm"""

    def __init__(self, h_speed_range: Tuple[int, int], v_speed_range: Tuple[int, int],
                 dc_factor_range: Tuple[int, int], midpoint: float):
        self.h_speed_range = h_speed_range
        self.v_speed_range = v_speed_range
        self.dc_factor_range = dc_factor_range
        self.midpoint = midpoint

    def sigmoid_transform(self, t: float) -> float:
        """Sigmoid interpolation curve"""
        return 1.0 / (1.0 + math.exp(-0.5 * (t - 0.3)))

    def bezier_transform(self, start: float, end: float, t: float) -> float:
        """Bezier curve interpolation"""
        return (1 - t) * (1 - t) * start + 2 * (1 - t) * t * 1.0 + t * t * end

    def calculate_factor(self, rotation_diff: float, turn_speed: float) -> float:
        """Calculate interpolation factor for a rotation difference"""
        t = min(abs(rotation_diff) / 180.0, 1.0)

        if t > self.midpoint:
            # Use Bezier (faster)
            bezier_speed = self.bezier_transform(0.05, 1.0, 1.0 - t)
            return bezier_speed * turn_speed
        else:
            # Use Sigmoid (slower, smoother)
            sigmoid_speed = self.sigmoid_transform(t)
            return sigmoid_speed * turn_speed

    def simulate_rotation(self, current_yaw: float, current_pitch: float,
                         target_yaw: float, target_pitch: float) -> Tuple[float, float]:
        """
        Simulate one tick of rotation toward target
        Returns: (new_yaw, new_pitch)
        """
        # Random speed from range (Gaussian distribution)
        h_speed = random.gauss(
            (self.h_speed_range[0] + self.h_speed_range[1]) / 2,
            (self.h_speed_range[1] - self.h_speed_range[0]) / 4
        )
        h_speed = max(self.h_speed_range[0], min(self.h_speed_range[1], h_speed)) / 100.0

        v_speed = random.gauss(
            (self.v_speed_range[0] + self.v_speed_range[1]) / 2,
            (self.v_speed_range[1] - self.v_speed_range[0]) / 4
        )
        v_speed = max(self.v_speed_range[0], min(self.v_speed_range[1], v_speed)) / 100.0

        # Calculate deltas
        yaw_diff = target_yaw - current_yaw
        pitch_diff = target_pitch - current_pitch

        # Normalize yaw diff to -180..180
        while yaw_diff > 180:
            yaw_diff -= 360
        while yaw_diff < -180:
            yaw_diff += 360

        # Calculate factors
        h_factor = self.calculate_factor(yaw_diff, h_speed)
        v_factor = self.calculate_factor(pitch_diff, v_speed)

        # Apply factors
        new_yaw = current_yaw + (h_factor * abs(yaw_diff) * (1 if yaw_diff > 0 else -1))
        new_pitch = current_pitch + (v_factor * abs(pitch_diff) * (1 if pitch_diff > 0 else -1))

        # Clamp pitch
        new_pitch = max(-90, min(90, new_pitch))

        return new_yaw, new_pitch

    def get_mode_name(self) -> str:
        return "Interpolation"


def test_autoconfig(sample_file: Path):
    """Main test function"""

    # 1. Load samples
    print(f"Loading samples from {sample_file}...")
    with open(sample_file) as f:
        data = json.load(f)

    samples = []
    for item in data:
        combat = item.get('combat', {})
        # Keys: a=current, b=previous, c=target, d=delta/velocity, g=playerDiff, h=targetDiff
        current_vec = combat.get('a', {})    # CURRENT_DIRECTION_VECTOR
        target_vec = combat.get('c', {})     # TARGET_DIRECTION_VECTOR
        velocity_delta = combat.get('d', {}) # DELTA_VECTOR

        sample = KillAuraConfigSample(
            combat_data=CombatSample(
                current_vector=Vec3(current_vec.get('x', 0), current_vec.get('y', 0), current_vec.get('z', 0)),
                target_vector=Vec3(target_vec.get('x', 0), target_vec.get('y', 0), target_vec.get('z', 0)),
                velocity_delta=Vec2(velocity_delta.get('x', 0), velocity_delta.get('y', 0))
            ),
            raycast_hit=item.get('raycast_hit', False)
        )
        samples.append(sample)

    print(f"Loaded {len(samples)} samples")

    test_samples = [s for s in samples if not s.raycast_hit][:200]

    # 2. Test all modes with random optimization (no defaults, no smart guess)
    print("\n=== Random Search Optimization for All Modes ===")
    modes_to_test = [
        'linear',
        'sigmoid',
        'interpolation',
        'acceleration',
    ]

    all_optimized = []

    for mode_name in modes_to_test:
        print(f"\n--- Optimizing {mode_name.capitalize()} with Random Search ---")
        opt_settings, opt_accuracy = random_optimize(test_samples, mode_name, iterations=20000)
        all_optimized.append((mode_name, opt_settings, opt_accuracy))
        print(f"{mode_name.capitalize()} Best: {opt_accuracy:.2f}%")

    # Find absolute best across all modes
    best_overall_mode, best_overall_settings, best_overall_accuracy = max(all_optimized, key=lambda x: x[2])

    # Refine the best result with focused random search around it
    print(f"\n=== Refining Best Mode ({best_overall_mode.capitalize()}) ===")
    refined_settings, refined_accuracy = refine_settings(best_overall_settings, test_samples, best_overall_mode, iterations=10000)

    print(f"\n{'='*60}")
    print(f"BEST CONFIGURATION FOUND ACROSS ALL MODES")
    print(f"{'='*60}")
    print(f"Mode: {best_overall_mode.upper()}")
    print(f"Horizontal Speed: {refined_settings['horizontal_speed'][0]}..{refined_settings['horizontal_speed'][1]}%")
    print(f"Vertical Speed: {refined_settings['vertical_speed'][0]}..{refined_settings['vertical_speed'][1]}%")
    print(f"Direction Change: {refined_settings['direction_change_factor'][0]}..{refined_settings['direction_change_factor'][1]}%")
    print(f"Midpoint: {refined_settings['midpoint']:.2f}")
    print(f"\nFINAL ACCURACY: {refined_accuracy:.2f}%")
    print(f"Total configurations tested: ~50000")
    print(f"{'='*60}")


def refine_settings(base_settings: Dict, test_samples: List[KillAuraConfigSample], mode: str, iterations: int = 10000) -> Tuple[Dict, float]:
    """Focused random search around best settings found"""

    best_settings = base_settings.copy()
    best_accuracy = evaluate_settings(base_settings, test_samples, mode)

    print(f"Refining from: {best_accuracy:.2f}%")
    print(f"Running {iterations} focused random iterations...")

    h_base = base_settings['horizontal_speed']
    v_base = base_settings['vertical_speed']
    dc_base = base_settings['direction_change_factor']
    mp_base = base_settings['midpoint']

    improvements = 0

    for i in range(iterations):
        # Random variations within ±20% of base values
        h_start = max(1, h_base[0] + random.randint(-20, 20))
        h_end = max(h_start + 5, min(100, h_base[1] + random.randint(-20, 20)))

        v_start = max(1, v_base[0] + random.randint(-20, 20))
        v_end = max(v_start + 5, min(100, v_base[1] + random.randint(-20, 20)))

        dc_start = max(50, dc_base[0] + random.randint(-15, 15))
        dc_end = max(dc_start + 5, min(100, dc_base[1] + random.randint(-15, 15)))

        # Random midpoint around base ±0.3
        midpoint = max(0.0, min(1.0, mp_base + random.uniform(-0.3, 0.3)))

        test_config = {
            'horizontal_speed': (h_start, h_end),
            'vertical_speed': (v_start, v_end),
            'direction_change_factor': (dc_start, dc_end),
            'midpoint': midpoint,
        }

        accuracy = evaluate_settings(test_config, test_samples, mode)

        if accuracy > best_accuracy:
            best_accuracy = accuracy
            best_settings = test_config
            improvements += 1
            if improvements % 5 == 0 or improvements <= 3:
                print(f"  Refinement #{improvements} (iter {i}): {accuracy:.2f}% (mp={midpoint:.2f}, h={h_start}-{h_end}, v={v_start}-{v_end})")

    print(f"Found {improvements} refinements across {iterations} iterations")

    final_accuracy = evaluate_settings(best_settings, test_samples, mode)
    return best_settings, final_accuracy


def random_optimize(test_samples: List[KillAuraConfigSample], mode: str, iterations: int = 20000) -> Tuple[Dict, float]:
    """Pure random search optimization - explores massive random space"""

    best_settings = None
    best_accuracy = 0.0
    improvements = 0

    print(f"Running {iterations} random iterations (exploring full parameter space)...")

    for i in range(iterations):
        # Generate completely random settings with wider ranges
        h_start = random.randint(1, 90)
        h_end = h_start + random.randint(3, 40)
        h_end = min(100, h_end)

        v_start = random.randint(1, 90)
        v_end = v_start + random.randint(3, 40)
        v_end = min(100, v_end)

        dc_start = random.randint(40, 95)
        dc_end = dc_start + random.randint(3, 20)
        dc_end = min(100, dc_end)

        # Full random midpoint range
        midpoint = random.uniform(0.0, 1.0)

        test_config = {
            'horizontal_speed': (h_start, h_end),
            'vertical_speed': (v_start, v_end),
            'direction_change_factor': (dc_start, dc_end),
            'midpoint': midpoint,
        }

        accuracy = evaluate_settings(test_config, test_samples, mode)

        # No bonus - pure accuracy only
        if accuracy > best_accuracy:
            best_accuracy = accuracy
            best_settings = test_config
            improvements += 1
            if improvements % 10 == 0 or improvements <= 5:
                print(f"  Improvement #{improvements} (iter {i}): {accuracy:.2f}% (mp={midpoint:.2f}, h={h_start}-{h_end}, v={v_start}-{v_end})")

    print(f"Found {improvements} improvements across {iterations} iterations")

    final_accuracy = evaluate_settings(best_settings, test_samples, mode)
    return best_settings, final_accuracy


def evaluate_settings(settings: Dict, test_samples: List[KillAuraConfigSample], mode: str = 'interpolation') -> float:
    """Evaluate settings and return accuracy percentage"""

    # Create appropriate simulator based on mode
    if mode == 'linear':
        simulator = LinearSimulator(
            (settings['horizontal_speed'][0], settings['horizontal_speed'][1]),
            (settings['vertical_speed'][0], settings['vertical_speed'][1])
        )
    elif mode == 'sigmoid':
        simulator = SigmoidSimulator(
            (settings['horizontal_speed'][0], settings['horizontal_speed'][1]),
            (settings['vertical_speed'][0], settings['vertical_speed'][1]),
            steepness=10.0,
            midpoint=settings['midpoint']
        )
    elif mode == 'acceleration':
        simulator = AccelerationSimulator(
            (settings['horizontal_speed'][0], settings['horizontal_speed'][1]),
            (settings['vertical_speed'][0], settings['vertical_speed'][1])
        )
    else:  # interpolation
        simulator = InterpolationSimulator(
            settings['horizontal_speed'],
            settings['vertical_speed'],
            settings['direction_change_factor'],
            settings['midpoint']
        )

    total_error_yaw = 0.0
    total_error_pitch = 0.0
    num_tested = 0

    for sample in test_samples:
        actual_moved_yaw = sample.combat_data.velocity_delta.x
        actual_moved_pitch = sample.combat_data.velocity_delta.y
        remaining_yaw = sample.combat_data.total_delta_yaw
        remaining_pitch = sample.combat_data.total_delta_pitch

        # Exclude extremely low values
        if abs(remaining_yaw) < 8.0 or abs(remaining_pitch) < 8.0:
            continue
        if abs(actual_moved_yaw) < 1.0 and abs(actual_moved_pitch) < 1.0:
            continue

        # Simulate
        sim_yaw, sim_pitch = simulator.simulate_rotation(0, 0, remaining_yaw, remaining_pitch)

        # Calculate error
        error_yaw = abs(sim_yaw - actual_moved_yaw)
        error_pitch = abs(sim_pitch - actual_moved_pitch)

        total_error_yaw += error_yaw
        total_error_pitch += error_pitch
        num_tested += 1

    if num_tested > 0:
        avg_error_yaw = total_error_yaw / num_tested
        avg_error_pitch = total_error_pitch / num_tested

        yaw_match = max(0, 100 - (avg_error_yaw * 2))
        pitch_match = max(0, 100 - (avg_error_pitch * 2))
        return (yaw_match + pitch_match) / 2
    return 0.0


def optimize_settings(base_settings: Dict, test_samples: List[KillAuraConfigSample], mode: str) -> Tuple[Dict, float]:
    """Optimize settings using grid search with bias toward HIGH midpoints (>0.75)"""

    best_settings = base_settings.copy()
    best_accuracy = evaluate_settings(base_settings, test_samples, mode)

    print(f"Starting from baseline: {best_accuracy:.2f}%")

    # Grid search parameters
    h_base = base_settings['horizontal_speed']
    v_base = base_settings['vertical_speed']
    dc_base = base_settings['direction_change_factor']
    mp_base = base_settings['midpoint']

    # Test variations
    h_variations = [
        (h_base[0] - 10, h_base[1] - 10),
        (h_base[0] - 5, h_base[1] - 5),
        h_base,
        (h_base[0] + 5, h_base[1] + 5),
        (h_base[0] + 10, h_base[1] + 10),
    ]

    v_variations = [
        (v_base[0] - 10, v_base[1] - 10),
        (v_base[0] - 5, v_base[1] - 5),
        v_base,
        (v_base[0] + 5, v_base[1] + 5),
        (v_base[0] + 10, v_base[1] + 10),
    ]

    dc_variations = [
        (dc_base[0] - 10, dc_base[1] - 5),
        (dc_base[0] - 5, dc_base[1]),
        dc_base,
        (dc_base[0] + 5, dc_base[1] + 5),
        (dc_base[0] + 10, dc_base[1] + 10),
    ]

    # PRIORITIZE HIGH midpoints (>0.75) for faster aiming
    high_midpoints = [0.95, 0.90, 0.85, 0.80, 0.75]  # Test these first
    medium_midpoints = [mp_base + 0.10, mp_base + 0.05, mp_base]
    low_midpoints = [mp_base - 0.05, mp_base - 0.10]

    # Combine: high priority first
    midpoint_variations = high_midpoints + medium_midpoints + low_midpoints
    midpoint_variations = sorted(set(max(0.0, min(1.0, mp)) for mp in midpoint_variations), reverse=True)

    tested = 0
    improvements = 0

    for h, v, dc, mp in itertools.product(h_variations, v_variations, dc_variations, midpoint_variations):
        # Clamp values
        h = (max(1, h[0]), min(100, h[1]))
        v = (max(1, v[0]), min(100, v[1]))
        dc = (max(50, dc[0]), min(100, dc[1]))
        mp = max(0.0, min(1.0, mp))

        test_config = base_settings.copy()
        test_config['horizontal_speed'] = h
        test_config['vertical_speed'] = v
        test_config['direction_change_factor'] = dc
        test_config['midpoint'] = mp

        accuracy = evaluate_settings(test_config, test_samples, mode)

        # STRONG bonus for high midpoints (>0.75) to encourage faster aiming
        midpoint_bonus = 0.0
        if mp >= 0.75:
            midpoint_bonus = (mp - 0.75) * 2.0  # Up to 0.5% bonus for mp=1.0
        adjusted_accuracy = accuracy + midpoint_bonus

        tested += 1

        if adjusted_accuracy > best_accuracy:
            best_accuracy = adjusted_accuracy
            best_settings = test_config
            improvements += 1
            print(f"  Improvement #{improvements}: {accuracy:.2f}% (bonus: +{midpoint_bonus:.2f}%, h={h}, v={v}, mp={mp:.2f})")

    print(f"Tested {tested} configurations, found {improvements} improvements")

    # Return actual accuracy without bonus
    final_accuracy = evaluate_settings(best_settings, test_samples, mode)
    return best_settings, final_accuracy


def micro_optimize(base_settings: Dict, test_samples: List[KillAuraConfigSample], mode: str) -> Tuple[Dict, float]:
    """Fine-tune settings with smaller increments for maximum accuracy"""

    best_settings = base_settings.copy()
    best_accuracy = evaluate_settings(base_settings, test_samples, mode)

    print(f"Starting micro-optimization from: {best_accuracy:.2f}%")

    iterations = 0
    max_iterations = 5

    while iterations < max_iterations:
        iterations += 1
        print(f"\nIteration {iterations}:")

        improved = False
        h_base = best_settings['horizontal_speed']
        v_base = best_settings['vertical_speed']
        dc_base = best_settings['direction_change_factor']
        mp_base = best_settings['midpoint']

        # Fine-grained variations (±1-3%)
        h_micro = [
            (h_base[0] - 3, h_base[1] - 3),
            (h_base[0] - 2, h_base[1] - 2),
            (h_base[0] - 1, h_base[1] - 1),
            h_base,
            (h_base[0] + 1, h_base[1] + 1),
            (h_base[0] + 2, h_base[1] + 2),
            (h_base[0] + 3, h_base[1] + 3),
        ]

        v_micro = [
            (v_base[0] - 3, v_base[1] - 3),
            (v_base[0] - 2, v_base[1] - 2),
            (v_base[0] - 1, v_base[1] - 1),
            v_base,
            (v_base[0] + 1, v_base[1] + 1),
            (v_base[0] + 2, v_base[1] + 2),
            (v_base[0] + 3, v_base[1] + 3),
        ]

        dc_micro = [
            (dc_base[0] - 2, dc_base[1] - 2),
            (dc_base[0] - 1, dc_base[1] - 1),
            dc_base,
            (dc_base[0] + 1, dc_base[1] + 1),
            (dc_base[0] + 2, dc_base[1] + 2),
        ]

        # Fine-tune midpoint (prefer >0.75)
        mp_micro = [mp_base + 0.03, mp_base + 0.02, mp_base + 0.01, mp_base, mp_base - 0.01, mp_base - 0.02]

        tested = 0
        for h, v, dc, mp in itertools.product(h_micro, v_micro, dc_micro, mp_micro):
            h = (max(1, h[0]), min(100, h[1]))
            v = (max(1, v[0]), min(100, v[1]))
            dc = (max(50, dc[0]), min(100, dc[1]))
            mp = max(0.0, min(1.0, mp))

            test_config = base_settings.copy()
            test_config['horizontal_speed'] = h
            test_config['vertical_speed'] = v
            test_config['direction_change_factor'] = dc
            test_config['midpoint'] = mp

            accuracy = evaluate_settings(test_config, test_samples, mode)

            # Bonus for mp > 0.75
            midpoint_bonus = (mp - 0.75) * 2.0 if mp >= 0.75 else 0.0
            adjusted_accuracy = accuracy + midpoint_bonus

            tested += 1

            if adjusted_accuracy > best_accuracy:
                best_accuracy = adjusted_accuracy
                best_settings = test_config
                improved = True
                print(f"  Micro improvement: {accuracy:.2f}% (mp={mp:.2f}, h={h}, v={v})")

        print(f"  Tested {tested} micro-variations")

        if not improved:
            print(f"  No improvements found, stopping")
            break

    final_accuracy = evaluate_settings(best_settings, test_samples, mode)
    return best_settings, final_accuracy


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1:
        sample_file = Path(sys.argv[1])
    else:
        sample_file = Path("debug-recorder/KillAuraConfig/samples.json")

    if not sample_file.exists():
        print(f"Sample file not found: {sample_file}")
        print("Please provide a valid path to a KillAuraConfigSample JSON file")
        print(f"Usage: {sys.argv[0]} <path-to-json-file>")
    else:
        test_autoconfig(sample_file)
