#!/usr/bin/env python3
import json
import math
from pathlib import Path

sample_file = Path("/storage/self/primary/FCL/.minecraft/versions/1.21.11-Fabric/LiquidBounce/debug-recorder/KillAuraConfig/2026-01-21_23-13-41.json")
with open(sample_file) as f:
    data = json.load(f)

total = 0
filtered_out_raycast = 0
filtered_out_small_remaining = 0
filtered_out_small_movement = 0
filtered_out_wrong_direction = 0
kept = 0

for item in data:
    total += 1

    if item.get('raycast_hit', False):
        filtered_out_raycast += 1
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

    # Filter 1: significant remaining
    if abs(yaw_diff) < 10.0 or abs(pitch_diff) < 10.0:
        filtered_out_small_remaining += 1
        continue

    # Filter 2: significant movement
    if abs(actual_yaw) < 2.0 or abs(actual_pitch) < 2.0:
        filtered_out_small_movement += 1
        continue

    # Filter 3: correct direction
    if (yaw_diff > 0) != (actual_yaw > 0):
        filtered_out_wrong_direction += 1
        continue
    if (pitch_diff > 0) != (actual_pitch > 0):
        filtered_out_wrong_direction += 1
        continue

    kept += 1

print(f"Total samples: {total}")
print(f"Filtered out - raycast hit: {filtered_out_raycast}")
print(f"Filtered out - small remaining (<10°): {filtered_out_small_remaining}")
print(f"Filtered out - small movement (<2°): {filtered_out_small_movement}")
print(f"Filtered out - wrong direction: {filtered_out_wrong_direction}")
print(f"KEPT for optimization: {kept}")
print(f"Kept percentage: {100*kept/total:.1f}%")
