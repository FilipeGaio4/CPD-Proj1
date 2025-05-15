#!/bin/bash
echo "Compiling..."
javac TCPServer/**/*.java
echo "Running the server..."
java TCPServer.lobby.LobbyServer
