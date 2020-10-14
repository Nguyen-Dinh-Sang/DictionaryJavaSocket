/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package musicinformationserver;

import musicinformationserver.socket.SocketServer;

/**
 *
 * @author DinhSang
 */
/*
    - server socket tách biệt với main.
    - main sử dụng các phương thức startServer và closeServer để làm việc với server socket.
 */
public class MusicInformationServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SocketServer server = new SocketServer(5000);
        server.startServer();
//        server.closeServer();
    }
    
}
