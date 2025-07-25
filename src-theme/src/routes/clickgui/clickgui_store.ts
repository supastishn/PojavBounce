import {type Writable, writable} from "svelte/store";

export interface TDescription {
    description: string;
    anchor: "left" | "right",
    x: number;
    y: number;
}

export const description: Writable<TDescription | null> = writable(null);

export const maxPanelZIndex: Writable<number> = writable(0);

export const highlightModuleName: Writable<string | null> = writable(null);

export const scaleFactor: Writable<number> = writable(2);

export const showGrid: Writable<boolean> = writable(false); // Grid disabled by default as requested

export const snappingEnabled: Writable<boolean> = writable(false); // Snapping disabled to match grid removal

export const gridSize: Writable<number> = writable(10);