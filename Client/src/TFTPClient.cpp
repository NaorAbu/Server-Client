//
// Created by Naor on 1/16/2017.
//

#include <stdlib.h>
#include <iostream>
#include <queue>
#include "CinClient.cpp"
#include <boost/thread.hpp>


int main(int argc, char *argv[]) {

    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    CinClient cin(&connectionHandler);
    boost::thread thr(boost::bind(&CinClient::readLine, &cin));


    while (ConnectionHandler::connected) {
        connectionHandler.decode();
    }
    thr.join();
    return 0;
}

