#!/usr/bin/env python3
"""
Validate that the Python simulator matches actual Kotlin behavior by comparing
simulated movements with recorded velocity deltas
"""

import json
import math
import sys
from pathlib import Path
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


class RotationSimulator:
    """Simulator matching Kotlin SigmoidAngleSmooth"""

    def __init__(self, h_speed: float, v_speed: float, midpoint: float, steepness: float = 10.0):
        self.h_speed = h_speed
        self.v_speed = v_speed
        self.midpoint = midpoint
        self.steepness = steepness

    def simulate(self, yaw_diff: float, pitch_diff: float):
        """Simulate one rotation step"""
        rotation_diff = math.sqrt(yaw_diff**2 + pitch_diff**2)

        if rotation_diff == 0:
            return 0.0, 0.0

        # Calculate sigmoid factor
        scaled = rotation_diff / 120.0
        sigmoid = 1.0 / (1.0 + math.exp(-self.steepness * (scaled - self.midpoint)))

        # Calculate factors (speed in degrees)
        h_factor = sigmoid * self.h_speed
        v_factor = sigmoid * self.v_speed

        # Apply straight-line normalization
        straight_line_yaw = abs(yaw_diff / rotation_diff) * h_factor
        straight_line_pitch = abs(pitch_diff / rotation_diff) * v_factor

        # Coerce to prevent overshoot
        actual_yaw = max(-straight_line_yaw, min(straight_line_yaw, yaw_diff))
        actual_pitch = max(-straight_line_pitch, min(straight_line_pitch, pitch_diff))

        return actual_yaw, actual_pitch


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 validate_simulator.py <path-to-json>")
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

        sample = CombatSample(
            current_vector=Vec3(current_vec.get('x', 0), current_vec.get('y', 0), current_vec.get('z', 0)),
            target_vector=Vec3(target_vec.get('x', 0), target_vec.get('y', 0), target_vec.get('z', 0)),
            velocity_delta=Vec2(velocity_delta.get('x', 0), velocity_delta.get('y', 0))
        )

        if not item.get('raycast_hit', False):
            samples.append(sample)

    print(f"Loaded {len(samples)} non-raycast samples\n")

    # Test with best config from grid search: h=1% (0.01*180=1.8°), v=91% (0.91*180=163.8°), mp=0.80
    simulator = RotationSimulator(h_speed=1.8, v_speed=163.8, midpoint=0.80)

    print("Comparing simulated vs actual movements (first 20 samples):")
    print("=" * 80)

    matches = 0
    total = 0
    total_error = 0.0

    for i, sample in enumerate(samples[:20]):
        remaining_yaw = sample.total_delta_yaw
        remaining_pitch = sample.total_delta_pitch
        actual_yaw = sample.velocity_delta.x
        actual_pitch = sample.velocity_delta.y

        # Filter extremely low values
        if abs(remaining_yaw) < 8.0 or abs(remaining_pitch) < 8.0:
            continue
        if abs(actual_yaw) < 1.0 and abs(actual_pitch) < 1.0:
            continue

        sim_yaw, sim_pitch = simulator.simulate(remaining_yaw, remaining_pitch)

        error_yaw = abs(sim_yaw - actual_yaw)
        error_pitch = abs(sim_pitch - actual_pitch)
        total_error_sample = error_yaw + error_pitch

        total_error += total_error_sample
        total += 1

        print(f"Sample {i+1}:")
        print(f"  Remaining: yaw={remaining_yaw:.2f}°, pitch={remaining_pitch:.2f}°")
        print(f"  Simulated: yaw={sim_yaw:.2f}°, pitch={sim_pitch:.2f}°")
        print(f"  Actual:    yaw={actual_yaw:.2f}°, pitch={actual_pitch:.2f}°")
        print(f"  Error:     yaw={error_yaw:.2f}°, pitch={error_pitch:.2f}° (total={total_error_sample:.2f}°)")

        if total_error_sample < 5.0:
            matches += 1
            print(f"  ✓ GOOD (error < 5°)")
        else:
            print(f"  ✗ HIGH ERROR")
        print()

    if total > 0:
        avg_error = total_error / total
        match_rate = (matches / total) * 100

        print("=" * 80)
        print(f"Validation Results:")
        print(f"  Total tested: {total}")
        print(f"  Average error: {avg_error:.2f}°")
        print(f"  Good matches (<5° error): {matches}/{total} ({match_rate:.1f}%)")
        print(f"  Accuracy: {max(0, 100 - avg_error):.2f}%")


if __name__ == "__main__":
    main()
