# Little Conception Server

## GameServer
accept the client connection

## GameMap
Stores everything about the map

*   walls
*	spawn points
*	attributes (allowed actions)

## GameControler
Handles PlayerAction to the map

## Map provider
returns a map for a player to play on ;
may wait before returning a map (for multiple players)

