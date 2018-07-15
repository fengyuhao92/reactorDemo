package com.fyh.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

//定义reactor，其中包括Selector和ServerSocketChannel
//将ServerSocketChannel和事件类型绑定到Seletor上，设置  serverSocket为非阻塞
class Reactor implements Runnable {
    final Selector selector;
    final ServerSocketChannel serverSocket;

    Reactor(int port) throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);
        SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        sk.attach(new Acceptor());
    }
    /*
     * Alternatively, use explicit SPI provider: SelectorProvider p =
     * SelectorProvider.provider(); selector = p.openSelector();
     * serverSocket = p.openServerSocketChannel();
     */

    // class Reactor continued
    //无限循环等待网络请求的到来
    //其中selector.select();会阻塞直到有绑定到selector的请求类型对应的请求到来，一旦收到事件，处理分发到对应的handler，并将这个事件移除
    public void run() { // normally in a new Thread
        try {
            while (!Thread.interrupted()) {
                selector.select();
                Set selected = selector.selectedKeys();
                Iterator it = selected.iterator();
                while (it.hasNext())
                    dispatch((SelectionKey)(it.next()));
                selected.clear();
            }
        } catch (IOException ex) {
            /* ... */ }
    }



    void dispatch(SelectionKey k) {
        Runnable r = (Runnable) (k.attachment());
        if (r != null)
            r.run();
    }

    // class Reactor continued
    class Acceptor implements Runnable { // inner
        public void run() {
            try {
                SocketChannel c = serverSocket.accept();
                if (c != null)
                    new Handler(selector, c);
            } catch (IOException ex) {
                /* ... */ }
        }
    }

    public static void main(String[] args) throws Exception{
        Reactor reactor = new Reactor(3160);
        Thread thread = new Thread(reactor);
        thread.start();
    }

}