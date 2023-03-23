import java.io.IOException;
import java.net.*;

/**
 * @author José Vinícius de Carvalho Oliveira
 */
public class Client {
    private int id;

    public Client(int id) {
        this.id = id;
    }

    /**
     * Realiza a lógica de envio da requisição para acessar a região crítica 
     * @throws IOException
     * @throws InterruptedException
     */
    public void requisitarRegiaoCritica() throws IOException, InterruptedException {
        InetAddress group = InetAddress.getByName(ParametrosHelper.ENDERECO);
        MulticastSocket socket = new MulticastSocket(ParametrosHelper.PORTA);
        socket.joinGroup(group);

        // Envia uma mensagem para solicitar a região crítica
        String mensagemSolicitacao = id + " REQUEST";
        byte[] data = mensagemSolicitacao.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, group, ParametrosHelper.PORTA);
        socket.send(packet);

        // Recebe a resposta do servidor
        byte[] buffer = new byte[1024];
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String resposta = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Resposta do servidor para o cliente " + id + ": " + resposta);

        socket.leaveGroup(group);
        socket.close();
        Thread.sleep(ParametrosHelper.TIME);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        for (int i = 1; i <= ParametrosHelper.NUM_PROCESSOS; i++) {
            Client client = new Client(i);
            client.requisitarRegiaoCritica();
        }
    }
}
