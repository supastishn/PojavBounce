#!/usr/bin/env python3
"""
Grid Search Optimizer for KillAura - Simple, easy to port to Kotlin

This is the SIMPLEST optimization algorithm - just test every combination.
No fancy libraries, just nested loops.
"""

import json
import math
import random
from pathlib import Path
from typing import List, Dict, Tuple
from dataclasses import dataclass


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
        current_yaw = math.degrees(math.atan2(self.current_vector.x, self.current_vector.z))
        target_yaw = math.degrees(math.atan2(self.target_vector.x, self.target_vector.z))

        diff = target_yaw - current_yaw
        while diff > 180:
            diff -= 360
        while diff < -180:
            diff += 360
        return diff

    @property
    def total_delta_pitch(self) -> float:
        """Calculate pitch delta from current to target rotation vectors"""
        current_pitch = math.degrees(math.asin(-self.current_vector.y))
        target_pitch = math.degrees(math.asin(-self.target_vector.y))
        return target_pitch - current_pitch


@dataclass
class KillAuraConfigSample:
    combat_data: CombatSample
    raycast_hit: bool


class RotationSimulator:
    """Base simulator - implements Sigmoid mode matching Kotlin SigmoidAngleSmooth exactly"""

    def __init__(self, h_speed: float, v_speed: float, midpoint: float):
        self.h_speed = h_speed  # Speed in degrees (0-180)
        self.v_speed = v_speed  # Speed in degrees (0-180)
        self.midpoint = midpoint
        self.steepness = 10.0

    def simulate(self, yaw_diff: float, pitch_diff: float) -> Tuple[float, float]:
        """
        Simulate one rotation step - matches Kotlin's towardsLinear() behavior exactly

        This simulates:
        1. SigmoidAngleSmooth.calculateFactors() -> returns factors
        2. FactorAngleSmooth.process() -> calls towardsLinear()
        3. Rotation.towardsLinear() -> applies straight-line movement with coercion
        """
        rotation_diff = math.sqrt(yaw_diff**2 + pitch_diff**2)

        if rotation_diff == 0:
            return 0.0, 0.0

        # Calculate sigmoid factor (matches SigmoidAngleSmooth.computeFactor)
        scaled = rotation_diff / 120.0
        sigmoid = 1.0 / (1.0 + math.exp(-self.steepness * (scaled - self.midpoint)))

        # Calculate factors (speed in degrees) - matches calculateFactors()
        h_factor = sigmoid * self.h_speed
        v_factor = sigmoid * self.v_speed

        # Apply straight-line normalization - matches Rotation.towardsLinear()
        straight_line_yaw = abs(yaw_diff / rotation_diff) * h_factor
        straight_line_pitch = abs(pitch_diff / rotation_diff) * v_factor

        # Coerce to prevent overshoot - matches coerceIn()
        actual_yaw = max(-straight_line_yaw, min(straight_line_yaw, yaw_diff))
        actual_pitch = max(-straight_line_pitch, min(straight_line_pitch, pitch_diff))

        return actual_yaw, actual_pitch


def evaluate_config(h_speed: float, v_speed: float, midpoint: float, samples: List[KillAuraConfigSample]) -> float:
    """
    Evaluate a configuration and return accuracy percentage

    This is the core function that will be ported to Kotlin
    """
    simulator = RotationSimulator(h_speed, v_speed, midpoint)

    total_error = 0.0
    num_tested = 0

    for sample in samples:
        if sample.raycast_hit:
            continue

        remaining_yaw = sample.combat_data.total_delta_yaw
        remaining_pitch = sample.combat_data.total_delta_pitch
        actual_yaw = sample.combat_data.velocity_delta.x
        actual_pitch = sample.combat_data.velocity_delta.y

        # Filter out samples where player wasn't actively aiming
        # 1. Must have significant distance to target (not already on target)
        if abs(remaining_yaw) < 5.0 and abs(remaining_pitch) < 5.0:
            continue

        # 2. Must have actually moved (not completely idle)
        if abs(actual_yaw) < 0.5 and abs(actual_pitch) < 0.5:
            continue

        # 3. At least one axis should show decent movement toward target
        moving_toward_yaw = abs(remaining_yaw) > 8.0 and abs(actual_yaw) > 1.0 and (remaining_yaw > 0) == (actual_yaw > 0)
        moving_toward_pitch = abs(remaining_pitch) > 8.0 and abs(actual_pitch) > 1.0 and (remaining_pitch > 0) == (actual_pitch > 0)

        if not (moving_toward_yaw or moving_toward_pitch):
            continue

        # Simulate
        sim_yaw, sim_pitch = simulator.simulate(remaining_yaw, remaining_pitch)

        # Calculate error
        error = abs(sim_yaw - actual_yaw) + abs(sim_pitch - actual_pitch)
        total_error += error
        num_tested += 1

    if num_tested == 0:
        return 0.0

    avg_error = total_error / num_tested
    # Convert error to accuracy percentage (lower error = higher accuracy)
    accuracy = max(0.0, 100.0 - avg_error)
    return accuracy


def grid_search_optimize(samples: List[KillAuraConfigSample]) -> Dict:
    """
    Grid search optimizer - systematically test all combinations

    SIMPLE to port to Kotlin - just nested loops, no dependencies
    """

    print("Starting Grid Search Optimization (Sigmoid mode)")
    print("Testing all combinations...")

    # Define grid parameters - using DEGREES not percentages
    # Based on data analysis: yaw avg ~4.4°, pitch avg ~2.9°
    # With sigmoid reduction, we need higher base values
    # Test range: 1° to 60° in steps of 2°
    h_speed_values = list(range(1, 61, 2))      # 1°, 3°, 5°, ..., 59°
    v_speed_values = list(range(1, 61, 2))      # 1°, 3°, 5°, ..., 59°
    midpoint_values = [round(x * 0.05, 2) for x in range(0, 21)]  # 0.00, 0.05, 0.10, ..., 1.00

    best_h = 10.0
    best_v = 10.0
    best_mp = 0.5
    best_accuracy = 0.0

    total_tests = len(h_speed_values) * len(v_speed_values) * len(midpoint_values)
    tested = 0
    improvements = 0

    print(f"Total configurations to test: {total_tests}")
    print()

    # GRID SEARCH - three nested loops
    for h_speed in h_speed_values:
        for v_speed in v_speed_values:
            for midpoint in midpoint_values:
                tested += 1

                accuracy = evaluate_config(h_speed, v_speed, midpoint, samples)

                if accuracy > best_accuracy:
                    best_accuracy = accuracy
                    best_h = h_speed
                    best_v = v_speed
                    best_mp = midpoint
                    improvements += 1
                    print(f"  Improvement #{improvements} ({tested}/{total_tests}): {accuracy:.2f}% "
                          f"(h={h_speed}°, v={v_speed}°, mp={midpoint:.2f})")

                # Progress indicator every 1000 tests
                if tested % 1000 == 0:
                    print(f"  Progress: {tested}/{total_tests} ({100*tested/total_tests:.1f}%) - Best so far: {best_accuracy:.2f}%")

    print(f"\nGrid Search Complete: {tested} configurations tested, {improvements} improvements found")

    return {
        'horizontal_speed': best_h,
        'vertical_speed': best_v,
        'midpoint': best_mp,
        'accuracy': best_accuracy
    }


def main():
    import sys

    if len(sys.argv) < 2:
        print("Usage: python3 grid_search_optimizer.py <path-to-json>")
        return

    sample_file = Path(sys.argv[1])
    if not sample_file.exists():
        print(f"File not found: {sample_file}")
        return

    # Load samples
    print(f"Loading samples from {sample_file}...")
    with open(sample_file) as f:
        data = json.load(f)

    samples = []
    for item in data:
        combat = item.get('combat', {})
        current_vec = combat.get('a', {})
        target_vec = combat.get('c', {})
        velocity_delta = combat.get('d', {})

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

    # Filter to non-raycast samples
    test_samples = [s for s in samples if not s.raycast_hit]
    print(f"Using {len(test_samples)} non-raycast samples for testing\n")

    # Run grid search
    result = grid_search_optimize(test_samples)

    print(f"\n{'='*60}")
    print(f"BEST CONFIGURATION (SIGMOID)")
    print(f"{'='*60}")
    print(f"Horizontal Speed: {result['horizontal_speed']}°")
    print(f"Vertical Speed: {result['vertical_speed']}°")
    print(f"Midpoint: {result['midpoint']:.2f}")
    print(f"\nFINAL ACCURACY: {result['accuracy']:.2f}%")
    print(f"{'='*60}")


if __name__ == "__main__":
    main()
