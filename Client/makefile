CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lboost_locale -lboost_thread
OFILES:=bin/ConnectionHandler.o bin/TFTPClient.o bin/CinClient.o

all: TFTPclient
	g++ -o bin/TFTPclient $(OFILES) $(LDFLAGS)

TFTPclient: $(OFILES)

bin/CinClient.o: src/CinClient.cpp
	g++ $(CFLAGS) -o bin/CinClient.o src/CinClient.cpp
	
bin/TFTPClient.o: src/TFTPClient.cpp
	g++ $(CFLAGS) -o bin/TFTPClient.o src/TFTPClient.cpp
	
bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp
	

.PHONY: clean
clean:
	rm -f bin/*
