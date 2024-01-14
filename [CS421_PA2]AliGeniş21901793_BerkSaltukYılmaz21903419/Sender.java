package cs421pa2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;

public class Sender extends Thread {

    private static FileInputStream reader = null;
    private static DatagramSocket ds = null;
    private static InetAddress ip = null;

    public static void main(String[] args) {

        // Taking commands
        System.out.println("Welcome, let me send this file!");
        String path = args[0];
        int port = Integer.parseInt(args[1]);
        int N = Integer.parseInt(args[2]);
        int timeout = Integer.parseInt(args[3]);

        // Custom Runner Class
        class PacketSendingDriver implements Runnable {

            private DatagramPacket dp;

            public PacketSendingDriver(DatagramPacket dp) {
                this.dp = dp;
            }

            public void run() {
                try {
                    while (true) {
                        // Send packet
                        ds.send(dp);

                        // Wait for main thread notification or timeout
                        Thread.sleep(timeout);
                    }
                }

                // Stop if main thread interrupts this thread
                catch (InterruptedException e) {
                    return;
                } catch (IOException e) {
                    return;
                }
            }
        }

        try {
            ds = new DatagramSocket();
            ip = InetAddress.getByName("127.0.0.1");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Read the file
        File input = new File(path);

        byte[] file_to_bytes = new byte[(int) input.length()];

        try {
            reader = new FileInputStream(input);
            reader.read(file_to_bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[][] segmentated_file = new byte[(int) Math.ceil(file_to_bytes.length / 1022.0)][];

        int seq_no = 1;
        int start = 0;
        for (int i = 0; i < segmentated_file.length; i++) {

            segmentated_file[i] = (byte[]) Arrays.copyOfRange(file_to_bytes, start,
                    Math.min(file_to_bytes.length, start + 1022));

            ByteArrayOutputStream concat = new ByteArrayOutputStream();
            concat.write((seq_no >> 8) & 0xFF);
            concat.write(seq_no & 0xFF);
            try {
                concat.write(segmentated_file[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            segmentated_file[i] = concat.toByteArray();
            start += 1022;
            seq_no += 1;
        }

        // Hold on a little more, we 'bout to send those packets!
        System.out.println("Cool pic bro... I read it, I am sending it now.");

        // TreeSet is cool, it is basically a set to keep unique ACK numbers but like a
        // min heap, it also keeps the minimum value in it. This way we can set the
        // sender base to min value and send the packets starting from min seq numbered
        // packet.
        TreeSet<Integer> acks_not_received = new TreeSet<Integer>();

        for (byte[] single_segment : segmentated_file) {
            int ack_no = ((single_segment[0] & 0xFF) << 8) | (single_segment[1] & 0xFF);
            acks_not_received.add(ack_no);
        }

        // Until the last ACK received, this will be true.
        boolean being_transmitted = true;

        int sender_base = 1;
        int next_sequence_no = 1;

        // Using a HashMap for sequence no - thread pair to interrupt the thread with
        // the returned ACK no (same with next seq no) when an ACK received.
        HashMap<Integer, Thread> sent_packets = new HashMap<Integer, Thread>();

        while (being_transmitted) {
            while (next_sequence_no <= segmentated_file.length && next_sequence_no < sender_base + N) {
                Thread packetSenderThread = new Thread(
                        new PacketSendingDriver(new DatagramPacket(segmentated_file[next_sequence_no - 1],
                                segmentated_file[next_sequence_no - 1].length, ip, port)));

                sent_packets.put(next_sequence_no, packetSenderThread);
                packetSenderThread.start();

                next_sequence_no += 1;
            }
            byte[] received_payload = new byte[2];
            DatagramPacket gotchu = new DatagramPacket(received_payload, received_payload.length);

            try {
                ds.receive(gotchu);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int ACK_obtained = ((received_payload[0] & 0xFF) << 8) | (received_payload[1] & 0xFF);

            if (sender_base <= ACK_obtained && ACK_obtained < sender_base + N) {
                if (sent_packets.get(ACK_obtained) != null) {
                    sent_packets.get(ACK_obtained).interrupt();
                    sent_packets.remove(ACK_obtained);

                    acks_not_received.remove(ACK_obtained);
                }
            }

            if (!acks_not_received.isEmpty()) {
                sender_base = acks_not_received.first();
            } else {

                byte zero_byte = (byte) (0 & 0xFF);
                byte[] zero = new byte[2];
                zero[0] = zero_byte;
                zero[1] = zero_byte;

                DatagramPacket zero_packet = new DatagramPacket(zero, zero.length, ip, port);
                try {
                    ds.send(zero_packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                being_transmitted = false;
            }
        }

        System.out.println("Until the next time, take care comrade!");
    }

}
