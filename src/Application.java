import java.util.ArrayList; 

import java.io.*;
import java.net.*;

class CONFIG {
    public static String HOST = "localhost";
    public static int PORT = 5050; 
}

public class Application {
    public static void main(String[] args) {
        Server server = new Server();
        server.start();

        try { Thread.sleep(100); } 
        catch (InterruptedException e) { e.printStackTrace(); }

        System.out.println();

        Student student_1 = new Student("#0001", 100);

        student_1.register_card();
        System.out.println("[CLIENT] student ID: " + student_1.get_ID() + "; personal student's money: " + student_1.get_money() + ";");
        student_1.get_client_info();

        System.out.println();
    
        student_1.top_up_balance(50);
        System.out.println("[CLIENT] student ID: " + student_1.get_ID() + "; personal student's money: " + student_1.get_money() + ";");
        student_1.get_client_info();

        System.out.println();

        student_1.pay_for_the_fare();
        System.out.println("[CLIENT] student ID: " + student_1.get_ID() + "; personal student's money: " + student_1.get_money() + ";");
        student_1.get_client_info();

        System.out.println();

        student_1.receive_balance();
        System.out.println("[CLIENT] student ID: " + student_1.get_ID() + "; personal student's money: " + student_1.get_money() + ";");
        student_1.get_client_info();
    }
}

class Server extends Thread {
    private boolean thread_alive;
    private ServerSocket server_socket;
    private ArrayList<Card> cards; 

    public Server() {
        this.thread_alive = true;
        this.cards = new ArrayList<Card>();
    }

    @Override
    public void run() {
        while (thread_alive) {
            try {
                this.server_socket = new ServerSocket(CONFIG.PORT);
                System.out.println("server started successfully on port: " + CONFIG.PORT);
    
                while (true) {
                    Socket client_socket = this.server_socket.accept();
    
                    ObjectOutputStream socket_out = new ObjectOutputStream(client_socket.getOutputStream());
                    ObjectInputStream socket_in = new ObjectInputStream(client_socket.getInputStream());
    
                    Request request = (Request) socket_in.readObject();
                    handle_request(socket_out, request);
                }
            } catch (Exception err) { System.out.println(err); System.out.println("[error while server launching]"); }
        }
    }

    public void terminate_server() {
        try {
            this.server_socket.close();
            this.thread_alive = false;
        } catch (Exception err) { System.out.println("[error while server termination]"); }
    }

    public void handle_request(ObjectOutputStream socket_out, Request request) {
        try {
            switch_label:
            switch (request.request) {
                case "register_card":
                    boolean card_exists = false;
                    for (Card card : this.cards) {
                        if (card.student_ID.equals(request.student_ID)) { card_exists = true; }
                    }

                    if (card_exists) {
                        socket_out.writeObject( new Response("fail", "student already has a card") );
                    } else {
                        this.cards.add( new Card(request.student_ID, 0) );

                        socket_out.writeObject( new Response("ok", null) );
                    }
                    break;

                case "get_client_info":
                    for (Card card : this.cards) {
                        if (card.student_ID.equals(request.student_ID)) { 
                            socket_out.writeObject( new Response("ok", "[SERVER] student ID: " + card.student_ID + "; balance: " + card.balance + ";") );
                            break switch_label;
                        }
                    }

                    socket_out.writeObject( new Response("fail", "student hasn't yet registered the card") );
                    break;

                case "top_up_balance":
                    for (Card card : this.cards) {
                        if (card.student_ID.equals(request.student_ID)) {    
                            try { card.balance += Integer.parseInt(request.data); }
                            catch (Exception err) {
                                socket_out.writeObject( new Response("fail", "money format is incorrect") );
                                break switch_label;
                            }

                            socket_out.writeObject( new Response("ok", null) );
                            break switch_label;
                        }
                    }

                    socket_out.writeObject( new Response("fail", "student hasn't yet registered the card") );
                    break;

                case "pay_for_the_fare":
                    for (Card card : this.cards) {
                        if (card.student_ID.equals(request.student_ID)) { 
                            if (card.balance >= 10) {
                                card.balance -= 10;
                        
                                socket_out.writeObject( new Response("ok", null) );
                                break switch_label;
                            } else {
                                socket_out.writeObject( new Response("fail", "not enough money on the card") );
                                break switch_label;
                            }
                        }
                    }

                    socket_out.writeObject( new Response("fail", "student hasn't yet registered the card") );
                    break;

                case "receive_balance":
                    for (Card card : this.cards) {
                        if (card.student_ID.equals(request.student_ID)) { 
                            if (card.balance > 0) {
                                socket_out.writeObject( new Response("ok", String.valueOf(card.balance)) );

                                card.balance = 0;
                                break switch_label;
                            } else {
                                socket_out.writeObject( new Response("fail", "card is empty") );
                                break switch_label;
                            }
                        }
                    }

                    socket_out.writeObject( new Response("fail", "student hasn't yet registered the card") );
                    break;
            
                default:
                    break;
            }
        } catch (Exception err) { System.out.println("[error while handling a request]"); }
    }
}

class Student {
    private String ID;
    private int money;

    public Student(String ID, int money) {
        this.ID = ID; 
        this.money = money;
    }

    public String get_ID() { return this.ID; }

    public int get_money() { return this.money; }

    public void register_card() { 
        try {
            Socket socket = new Socket(CONFIG.HOST, CONFIG.PORT);

            ObjectOutputStream socket_out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream socket_in = new ObjectInputStream(socket.getInputStream());

            socket_out.writeObject( new Request(this.ID, "register_card", null) ); 
            Response response = (Response) socket_in.readObject();
            if (response.status.equals("fail")) {
                System.out.println("student " + this.ID + ": [request error: " + response.message + "]");
            }

            socket.close(); 
        } catch (Exception err) { System.out.println("student " + this.ID + ": [error while a request to the server]"); }
    }

    public void get_client_info() { 
        try {
            Socket socket = new Socket(CONFIG.HOST, CONFIG.PORT);

            ObjectOutputStream socket_out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream socket_in = new ObjectInputStream(socket.getInputStream());

            socket_out.writeObject( new Request(this.ID, "get_client_info", null) );
            Response response = (Response) socket_in.readObject();
            if (response.status.equals("fail")) {
                System.out.println("student " + this.ID + ": [request error: " + response.message + "]");
            } else { System.out.println(response.message); }

            socket.close(); 
        } catch (Exception err) { System.out.println("student " + this.ID + ": [error while a request to the server]"); }
    }

    public void top_up_balance(int money) { 
        try {
            if (this.money >= money) {
                Socket socket = new Socket(CONFIG.HOST, CONFIG.PORT);

                ObjectOutputStream socket_out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream socket_in = new ObjectInputStream(socket.getInputStream());
    
                socket_out.writeObject( new Request(this.ID, "top_up_balance", String.valueOf(money)) ); 
                Response response = (Response) socket_in.readObject();
                if (response.status.equals("fail")) {
                    System.out.println("student " + this.ID + ": [request error: " + response.message + "]");
                } else { this.money -= money; }

                socket.close(); 
            } else { System.out.println("[impossible to top up the balance because the student doesn't have enough money on the balance]"); }
        } catch (Exception err) { System.out.println("student " + this.ID + ": [error while a request to the server]"); }
    }

    public void pay_for_the_fare() { 
        try {
            Socket socket = new Socket(CONFIG.HOST, CONFIG.PORT);

            ObjectOutputStream socket_out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream socket_in = new ObjectInputStream(socket.getInputStream());

            socket_out.writeObject( new Request(this.ID, "pay_for_the_fare", null) );
            Response response = (Response) socket_in.readObject();
            if (response.status.equals("fail")) {
                System.out.println("student " + this.ID + ": [request error: " + response.message + "]");
            }

            socket.close(); 
        } catch (Exception err) { System.out.println("student " + this.ID + ": [error while a request to the server]"); }
    }

    public void receive_balance() { 
        try {
            Socket socket = new Socket(CONFIG.HOST, CONFIG.PORT);

            ObjectOutputStream socket_out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream socket_in = new ObjectInputStream(socket.getInputStream());

            socket_out.writeObject( new Request(this.ID, "receive_balance", null) ); 
            Response response = (Response) socket_in.readObject();
            if (response.status.equals("fail")) {
                System.out.println("student " + this.ID + ": [request error: " + response.message + "]");
            } else {
                this.money += Integer.parseInt(response.message);
            }

            socket.close(); 
        } catch (Exception err) { System.out.println("student " + this.ID + ": [error while a request to the server]"); }
    }
}

class Request implements Serializable {
    protected String student_ID;
    protected String request;
    protected String data;

    public Request(String student_ID, String request, String data) {
        this.student_ID = student_ID;
        this.request = request;
        this.data = data;
    }
}

class Response implements Serializable {
    protected String status;
    protected String message;

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
    }
}

class Card {
    protected String student_ID;
    protected int balance;

    public Card(String student_ID, int balance) {
        this.student_ID = student_ID;
        this.balance = balance;
    }
}