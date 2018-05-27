#include <iostream>
#include "boost/algorithm/string.hpp"
#include "../include/ConnectionHandler.h"
#include <sys/stat.h>

using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;

bool ConnectionHandler::sentDisc = false;


bool fileExists(const string &filename);

void shortToBytes(short num, char *bytesArr);

short bytesToShort(char *bytesArr);

bool ConnectionHandler::connected = true;


ConnectionHandler::ConnectionHandler(string host, short port) : host_(host), port_(port), io_service_(),
                                                                socket_(io_service_), fs(), fName() {}

ConnectionHandler::~ConnectionHandler() {
    close();
}

bool ConnectionHandler::connect() {
    std::cout << "Starting connect to "
              << host_ << ":" << port_ << std::endl;
    try {
        tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
        boost::system::error_code error;
        socket_.connect(endpoint, error);
        if (error)
            throw boost::system::system_error(error);
    }
    catch (std::exception &e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp) {
            tmp += socket_.read_some(boost::asio::buffer(bytes + tmp, bytesToRead - tmp), error);
        }
        if (error)
            throw boost::system::system_error(error);
    } catch (std::exception &e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp) {
            tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
        }
        if (error)
            throw boost::system::system_error(error);
    } catch (std::exception &e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getLine(std::string &line) {
    return getFrameAscii(line, '\n');
}

bool ConnectionHandler::sendLine(std::string &line) {
    return sendFrameAscii(line, '\n');
}

bool ConnectionHandler::getFrameAscii(std::string &frame, char delimiter) {
    char ch;
    // Stop when we encounter the null character. 
    // Notice that the null character is not appended to the frame string.
    try {
        do {
            getBytes(&ch, 1);
            frame.append(1, ch);
        } while (delimiter != ch);
    } catch (std::exception &e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendFrameAscii(const std::string &frame, char delimiter) {
    bool result = sendBytes(frame.c_str(), frame.length());
    if (!result) return false;
    return sendBytes(&delimiter, 1);
}

// Close down the connection properly.
void ConnectionHandler::close() {
    try {
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}

bool ConnectionHandler::decode() {
    char op[2];
    bool ans = true;
    ans = ConnectionHandler::getBytes(op, 2);
    if (!ans) {
        return false;
    }
    //getting DATA
    if (op[1] == 3) {
        char pSize[2];
        ans = getBytes(pSize, 2);
        if (!ans) {
            return ans;
        }
        char bNumArr[2];
        ans = getBytes(bNumArr, 2);
        if (!ans) {
            return ans;
        }
        short p = bytesToShort(pSize);
        char dat[p];
        ans = getBytes(dat, p);
        if (!ans) {
            return ans;
        }
        if (fs.is_open()) {
            fs.write(dat, sizeof(dat));
            if (sizeof(dat) < 512) {
                fs.close();
                cout<<"RRQ " + fName + " complete"<<endl;
            }
        } else {
            for (int i = 0; i < p; i++) {
                if(dat[i] != '\0'){
                    cout << dat[i];
                }
                else{
                    cout<< endl;
                }

            }
        }
        if (sizeof(dat) < 512) {

        }
        char answer[4];
        answer[0] = 0;
        answer[1] = 4;
        answer[2] = bNumArr[0];
        answer[3] = bNumArr[1];
        ans = sendBytes(answer, 4);
        if (!ans) {
            return ans;
        }
        //GEETING ACK
    } else if (op[1] == 4) {
        char bNumArr2[2];
        ans = getBytes(bNumArr2, 2);
        if (!ans) {
            return ans;
        }
        short bNum = bytesToShort(bNumArr2);
        cout << "> ACK " << bNum << endl;
        if (fs.is_open()) {
            //getting data from file
            char c;
            std::vector<char> data;
            int i = 0;
            while (i < 512 && !fs.eof()) {
                fs.get(c);
                data.push_back(c);
                i++;
            }
            //size of data to send
            char datamsg[6 + data.size()];

            //putting op code
            datamsg[0] = 0;
            datamsg[1] = 3;

            //putting packet size
            char pSize[2];
            shortToBytes((short)data.size(), pSize);
            datamsg[2] = pSize[0];
            datamsg[3] = pSize[1];

            //put block number
            char block[2];
            short nextbNum = bNum+1;
            shortToBytes(nextbNum, block);
            datamsg[4] = block[0];
            datamsg[5] = block[1];

            //moving vector to msg
            int index = 6;
            for (size_t k = 0; k < data.size(); k++) {
                datamsg[index] = data.at(k);
                index++;
            }

            if (i < 512) {
                fs.close();
            }
            ans = sendBytes(datamsg, 6+data.size());
            if (!ans) {
                return ans;
            }
        } else if (bNum == 0) {
            if (sentDisc) {
                ConnectionHandler::connected = false;
                close();
            }
        }else {
            cout<<"WRQ " + fName + " complete"<<endl;
        }
    } else if (op[1] == 5) {
        char errCode[2];
        ans = getBytes(errCode, 2);
        if (!ans) {
            return false;
        }
        short errNum = bytesToShort(errCode);
        cout << "> Error ";
        cout << errNum << endl;
        string errmsg;
        ans = getFrameAscii(errmsg, '\0');
        if (!ans) {
            return false;
        }
        if (fs.is_open()) {
            fs.close();
            std::remove(fName.c_str());
            fName = "";
        }
    } else if (op[1] == 9) {
        char addel[1];
        ans = getBytes(addel, 1);
        if (!ans) {
            return false;
        }
        string adddel = addel[0] == 1 ? "add" : "del";
        string filename;
        ans = getFrameAscii(filename, '\0');
        if (!ans) {
            return false;
        }
        cout << "> BCAST " + adddel + " " + filename << endl;
    }
    return  false;
}

//string from the user
bool ConnectionHandler::encode(string msg) {
    std::vector<string> parts;
    boost::split(parts, msg, boost::is_space());
    if (parts.at(0) == "RRQ") {

        int size = 2;
        char bytes[parts.at(1).length() + 3];
        bytes[0] = 0;
        bytes[1] = 1;
        int byteI = 2;
        for (size_t i = 0; i < parts.at(1).length(); i++) {
            bytes[byteI] = parts.at(1).c_str()[i];
            size++;
            byteI++;
        }
        bytes[byteI] = '\0';
        size++;
        if (!fileExists(parts.at(1))) {
            fs.open(parts.at(1), std::fstream::out);
            fName = parts.at(1);
        } else {
            cout << "File Already Exists" << endl;
            return false;
        }
        return completeAndSend(bytes, size);
    } else if (parts.at(0) == "WRQ") {
        if(parts.size() < 2){
            cout<< "Invalid Command<<" <<endl;
        } else{
            int size = 2;
            char bytes[parts.at(1).length() + 2];
            bytes[0] = 0;
            bytes[1] = 2;
            int byteI = 2;
            for (size_t i = 0; i < parts.at(1).length(); i++) {
                bytes[byteI] = parts.at(1).c_str()[i];
                size++;
                byteI++;
            }
            bytes[byteI] = '\0';
            size++;
            fs.open(parts.at(1), std::ios::in);
            if (!fs.good()) {
                cout << "No Such File" << endl;
                fs.close();
                return false;
            }
            fName = parts.at(1);
            return completeAndSend(bytes, size);
        }
    } else if (parts.at(0) == "DIRQ") {
        int size = 2;
        char bytes[2];
        bytes[0] = 0;
        bytes[1] = 6;
        return completeAndSend(bytes, size);
    } else if (parts.at(0) == "LOGRQ") {
        if(parts.size() < 2){
            cout<< "Invalid Command" <<endl;
        }
        else{
            int size = 2;
            char bytes[parts.at(1).length() + 2];
            bytes[0] = 0;
            bytes[1] = 7;
            int byteI = 2;
            for (size_t i = 0; i < parts.at(1).length(); i++) {
                bytes[byteI] = parts.at(1).c_str()[i];
                size++;
                byteI++;
            }
            bytes[byteI] = '\0';
            size++;
            return completeAndSend(bytes, size);
        }
    } else if (parts.at(0) == "DELRQ") {
        if(parts.size() < 2){
            cout<< "Invalid Command" <<endl;
        }
        else{
            int size = 2;
            char bytes[parts.at(1).length() + 3];
            bytes[0] = 0;
            bytes[1] = 8;
            int byteI = 2;
            for (size_t i = 0; i < parts.at(1).length(); i++) {
                bytes[byteI] = parts.at(1).c_str()[i];
                size++;
                byteI++;
            }
            bytes[byteI] = '\0';
            size++;
            return completeAndSend(bytes, size);
        }
    } else if (parts.at(0) == "DISC") {
        int size = 2;
        char bytes[2];
        bytes[0] = 0;
        bytes[1] = 10;
        sentDisc = true;
        return completeAndSend(bytes, size);
    } else {
        cout << "Invalid Command" <<endl;
        return false;
    }
    return false;
}

bool ConnectionHandler::completeAndSend(const char *bytes, int size) {
    if (sizeof(bytes) < 1) {
        return false;
    } else {
        return sendBytes(bytes, size);
    }
}

short check(char *bytesArr) {
    short result = (short) ((bytesArr[0] & 0xff) << 8);
    result += (short) (bytesArr[1] & 0xff);
    return result;
}

void shortToBytes(short num, char *bytesArr) {
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}

short bytesToShort(char *bytesArr) {
    short result = (short) ((bytesArr[0] & 0xff) << 8);
    result += (short) (bytesArr[1] & 0xff);
    return result;
}

bool fileExists(const string &filename) {
    struct stat buf;
    if (stat(filename.c_str(), &buf) != -1) {
        return true;
    }
    return false;
}
