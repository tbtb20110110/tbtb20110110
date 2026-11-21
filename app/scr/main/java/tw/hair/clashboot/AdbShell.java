package tw.haier.clashboot;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

final class AdbShell {
    private static final int ADB_VERSION = 0x01000001;
    private static final int ADB_MAXDATA = 4096;
    private static int code(String s) {
        return (s.charAt(0)&255) | ((s.charAt(1)&255)<<8) | ((s.charAt(2)&255)<<16) | ((s.charAt(3)&255)<<24);
    }
    private static void writePacket(DataOutputStream out, String cmd, int a0, int a1, byte[] data) throws IOException {
        int c = code(cmd), len = data == null ? 0 : data.length, sum = 0;
        if (data != null) for (byte b : data) sum += b & 255;
        writeLE(out,c); writeLE(out,a0); writeLE(out,a1); writeLE(out,len); writeLE(out,sum); writeLE(out,~c);
        if (len > 0) out.write(data);
        out.flush();
    }
    private static void writeLE(DataOutputStream out, int v) throws IOException {
        out.write(v & 255); out.write((v>>>8)&255); out.write((v>>>16)&255); out.write((v>>>24)&255);
    }
    private static int readLE(DataInputStream in) throws IOException {
        return (in.readUnsignedByte()) | (in.readUnsignedByte()<<8) | (in.readUnsignedByte()<<16) | (in.readUnsignedByte()<<24);
    }
    private static Packet readPacket(DataInputStream in) throws IOException {
        int cmd=readLE(in), a0=readLE(in), a1=readLE(in), len=readLE(in), sum=readLE(in), magic=readLE(in);
        if (magic != ~cmd || len < 0 || len > 1024*1024) throw new IOException("Invalid ADB packet");
        byte[] data=new byte[len]; in.readFully(data);
        return new Packet(cmd,a0,a1,data);
    }
    private static final class Packet {
        int cmd,a0,a1; byte[] data;
        Packet(int c,int x,int y,byte[] d){cmd=c;a0=x;a1=y;data=d;}
        String name(){ return new String(new char[]{(char)(cmd&255),(char)((cmd>>>8)&255),(char)((cmd>>>16)&255),(char)((cmd>>>24)&255)}); }
    }

    static String exec(String command) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1",5555),5000);
            socket.setSoTimeout(10000);
            DataInputStream in=new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            byte[] banner="host::features=shell_v2,cmd\n".getBytes(StandardCharsets.UTF_8);
            writePacket(out,"CNXN",ADB_VERSION,ADB_MAXDATA,banner);
            Packet p=readPacket(in);
            if (!p.name().equals("CNXN")) throw new IOException("ADB handshake expected CNXN, got "+p.name());
            byte[] service=("shell:"+command+"\0").getBytes(StandardCharsets.UTF_8);
            writePacket(out,"OPEN",1,0,service);
            StringBuilder result=new StringBuilder();
            while (true) {
                p=readPacket(in);
                String n=p.name();
                if ("OKAY".equals(n)) continue;
                if ("WRTE".equals(n)) {
                    result.append(new String(p.data,StandardCharsets.UTF_8));
                    writePacket(out,"OKAY",1,p.a0,null);
                } else if ("CLSE".equals(n)) {
                    writePacket(out,"CLSE",1,p.a0,null);
                    break;
                } else if ("FAIL".equals(n)) {
                    throw new IOException("ADB shell rejected request: "+new String(p.data,StandardCharsets.UTF_8));
                }
            }
            return result.toString();
        }
    }
}
