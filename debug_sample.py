#!/usr/bin/env python3
"""Debug single sample calculation"""
import json
import math

# Load one sample
with open("/storage/self/primary/FCL/.minecraft/versions/1.21.11-Fabric/LiquidBounce/debug-recorder/KillAuraConfig/2026-01-21_23-13-41.json") as f:
    data = json.load(f)

# Find first non-raycast sample with significant movement
for item in data:
    if item.get('raycast_hit', False):
        continue

    combat = item.get('combat', {})
    current_vec = combat.get('a', {})
    target_vec = combat.get('c', {})
    velocity_delta = combat.get('d', {})

    # Calculate remaining
    current_yaw = math.degrees(math.atan2(current_vec['x'], current_vec['z']))
    target_yaw = math.degrees(math.atan2(target_vec['x'], target_vec['z']))
    yaw_diff = target_yaw - current_yaw
    while yaw_diff > 180:
        yaw_diff -= 360
    while yaw_diff < -180:
        yaw_diff += 360

    current_pitch = math.degrees(math.asin(-current_vec['y']))
    target_pitch = math.degrees(math.asin(-target_vec['y']))
    pitch_diff = target_pitch - current_pitch

    actual_yaw = velocity_delta['x']
    actual_pitch = velocity_delta['y']

    if abs(yaw_diff) < 8 or abs(pitch_diff) < 8:
        continue
    if abs(actual_yaw) < 1 and abs(actual_pitch) < 1:
        continue

    print(f"Sample found:")
    print(f"  Remaining: yaw={yaw_diff:.2f}°, pitch={pitch_diff:.2f}°")
    print(f"  Actual:    yaw={actual_yaw:.2f}°, pitch={actual_pitch:.2f}°")

    # Calculate rotation diff
    rotation_diff = math.sqrt(yaw_diff**2 + pitch_diff**2)
    print(f"  Rotation diff: {rotation_diff:.2f}°")

    # Try different configs
    configs = [
        (1, 59, 0.70),
        (10, 10, 0.50),
        (actual_yaw * 3, actual_pitch * 3, 0.50),
    ]

    for h, v, mp in configs:
        # Simulate
        scaled = rotation_diff / 120.0
        sigmoid = 1.0 / (1.0 + math.exp(-10.0 * (scaled - mp)))

        h_factor = sigmoid * h
        v_factor = sigmoid * v

        straight_line_yaw = abs(yaw_diff / rotation_diff) * h_factor
        straight_line_pitch = abs(pitch_diff / rotation_diff) * v_factor

        sim_yaw = max(-straight_line_yaw, min(straight_line_yaw, yaw_diff))
        sim_pitch = max(-straight_line_pitch, min(straight_line_pitch, pitch_diff))

        error = abs(sim_yaw - actual_yaw) + abs(sim_pitch - actual_pitch)

        print(f"\n  Config h={h}°, v={v}°, mp={mp}:")
        print(f"    Sigmoid factor: {sigmoid:.3f}")
        print(f"    Simulated: yaw={sim_yaw:.2f}°, pitch={sim_pitch:.2f}°")
        print(f"    Error: {error:.2f}°")

    break
