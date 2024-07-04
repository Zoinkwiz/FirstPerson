# First Person

This is a Runelite plugin which enables a (janky) first-person perspective. There are two modes, the detached camera mode, and the GPU rendering mode.

## Default mode: Detached Camera

By default this plugin makes use of the Occulus mode of camera, you will be unable to interact with any NPCs or Objects whilst the plugin is on.

## GPU Rendering mode

Alternatively, you can make use of the 'GPU rendering' config setting. This will instead draw the scene entirely from scratch, much as the GPU plugin does, but from the player's head. 

**HOWEVER**, this will not adjust any other Runelite plugins and their overlays, meaning they will not align with your view. 

Additionally, although items, NPCs and objects will be interacted with normally, clicking on floor tiles will not align as you'd expect. It's strongly recommended to instead move around using the minimap.