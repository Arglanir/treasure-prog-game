# treasure-prog-game
A java programmation game:

- The teacher runs the server.
- The students create a bot for the game and connect their bot to the server.

You may check the [conception](src/fr/isae/mae/ss/sockets/treasures/server/Conception.md) 

## Returned
The server returns a string `X Y PPPPOPPPP I`

*   `X Y`: the position (0 0 is top left)
*   `POP`: what is around (`.` nothing, `#` a wall, `P` a player, `T` a treasure)
*   `I`: the intensity (depending on the treasure and the distance)

If a treasure is found, the server returns first a string `Found XXX gold. NNN treasures left`

## Actions
* `UP DOWN LEFT RIGHT`: go to this direction (if there is no wall)
* `EVAL`: After the intensity, get a list of every treasure content left
* `TRIGO`: Next intensity is an angle to the most intensity treasure (0: to the left, 90: to the bottom)
* `FLASH`: Next POP will have a radius 4 instead of 1
* `TELEPORT X Y`: You will go there (if there is no wall)
Not implemented yet:
* `CALL`: After the intensity, gives the list of players and their position
* `STUN`: Block players at distance 3 for some seconds

Note: not all actions may be enabled.

