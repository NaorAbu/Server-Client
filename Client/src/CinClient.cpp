//
// Created by Naor on 1/16/2017.
//


#include <queue>
#include <iostream>
#include "../include/ConnectionHandler.h"

class CinClient {
private:
    ConnectionHandler *handler;

public:
    void readLine() {
        while (!ConnectionHandler::sentDisc) {
            const short bufsize = 1024;
            char buf[bufsize];
            std::cin.getline(buf, bufsize);
            std::string line(buf);
            handler->encode(line);

        }
    }

    CinClient( ConnectionHandler *handler) : handler(handler) {}

};
