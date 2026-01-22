#!/usr/bin/env python3
"""
Analyze what the actual data looks like
"""

import json
import math
import sys
from pathlib import Path


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 analyze_data.py <path-to-json>")
        return

    sample_file = Path(sys.argv[1])
    with open(sample_file) as f:
        data = json.load(f)

    yaw_velocities = []
    pitch_velocities = []
    yaw_remainings = []
    pitch_remainings = []

    for item in data:
        if item.get('raycast_hit', False):
            continue

        combat = item.get('combat', {})
        current_vec = combat.get('a', {})
        target_vec = combat.get('c', {})
        velocity_delta = combat.get('d', {})

        # Calculate remaining
        current_yaw = math.degrees(math.atan2(current_vec.get('x', 0), current_vec.get('z', 0)))
        target_yaw = math.degrees(math.atan2(target_vec.get('x', 0), target_vec.get('z', 0)))
        yaw_diff = target_yaw - current_yaw
        while yaw_diff > 180:
            yaw_diff -= 360
        while yaw_diff < -180:
            yaw_diff += 360

        current_pitch = math.degrees(math.asin(-current_vec.get('y', 0)))
        target_pitch = math.degrees(math.asin(-target_vec.get('y', 0)))
        pitch_diff = target_pitch - current_pitch

        # Get actual velocity
        actual_yaw = velocity_delta.get('x', 0)
        actual_pitch = velocity_delta.get('y', 0)

        # Filter
        if abs(yaw_diff) < 8.0 or abs(pitch_diff) < 8.0:
            continue
        if abs(actual_yaw) < 1.0 and abs(actual_pitch) < 1.0:
            continue

        yaw_velocities.append(abs(actual_yaw))
        pitch_velocities.append(abs(actual_pitch))
        yaw_remainings.append(abs(yaw_diff))
        pitch_remainings.append(abs(pitch_diff))

    print(f"Analyzed {len(yaw_velocities)} samples\n")

    # Sort for percentiles
    yaw_velocities.sort()
    pitch_velocities.sort()

    print("YAW VELOCITY (actual movement per tick):")
    print(f"  Min:  {yaw_velocities[0]:.2f}°")
    print(f"  P25:  {yaw_velocities[len(yaw_velocities)//4]:.2f}°")
    print(f"  P50:  {yaw_velocities[len(yaw_velocities)//2]:.2f}°")
    print(f"  P75:  {yaw_velocities[3*len(yaw_velocities)//4]:.2f}°")
    print(f"  P90:  {yaw_velocities[9*len(yaw_velocities)//10]:.2f}°")
    print(f"  Max:  {yaw_velocities[-1]:.2f}°")
    print(f"  Avg:  {sum(yaw_velocities)/len(yaw_velocities):.2f}°")

    print("\nPITCH VELOCITY (actual movement per tick):")
    print(f"  Min:  {pitch_velocities[0]:.2f}°")
    print(f"  P25:  {pitch_velocities[len(pitch_velocities)//4]:.2f}°")
    print(f"  P50:  {pitch_velocities[len(pitch_velocities)//2]:.2f}°")
    print(f"  P75:  {pitch_velocities[3*len(pitch_velocities)//4]:.2f}°")
    print(f"  P90:  {pitch_velocities[9*len(pitch_velocities)//10]:.2f}°")
    print(f"  Max:  {pitch_velocities[-1]:.2f}°")
    print(f"  Avg:  {sum(pitch_velocities)/len(pitch_velocities):.2f}°")

    print("\nYAW REMAINING (distance to target):")
    print(f"  Avg:  {sum(yaw_remainings)/len(yaw_remainings):.2f}°")

    print("\nPITCH REMAINING (distance to target):")
    print(f"  Avg:  {sum(pitch_remainings)/len(pitch_remainings):.2f}°")

    # Check ratio
    print(f"\nRATIO OF ACTUAL VELOCITY:")
    print(f"  Yaw/Pitch ratio: {sum(yaw_velocities)/sum(pitch_velocities):.3f}")


if __name__ == "__main__":
    main()
