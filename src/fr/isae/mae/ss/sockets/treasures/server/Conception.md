# Little Conception Server

## GameServer
Accepts the client connection

Can be controlled using commands:
* ``help``: lists the available commands
* ``who``: lists the connected players, with scrore and more
* ``reload``: reload the list of maps (if new ones have been added)
* ``remove [player]``: remove a player
* ``disconnect [player]``: disconnects the given player
* ``activate [options]``: activates the given options, like:
  * ``allActions``: all actions authorized
  * ``action``: this action is authorized
  * ``action=off``: this action is not authorized anymore
* ``prepare [player1] [player2]... ``: prepares an arena for the specified players
* ``start [mapname]``: starts the arena with the given map 

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

