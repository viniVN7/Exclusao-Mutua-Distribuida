import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author José Vinícius de Carvalho Oliveira
 */
public class Server {
    private final Map<Integer, Integer> timestamps = new HashMap<>();
    private final MulticastSocket socket;
    private final InetAddress group;
    private final Queue<Integer> queue = new LinkedList<>();
    private int contadorRC = 0;
    private boolean isManterConexao = true;

    public Server() throws IOException {
        socket = new MulticastSocket(ParametrosHelper.PORTA);
        group = InetAddress.getByName(ParametrosHelper.ENDERECO);
        socket.joinGroup(group);
    }

    public void processarRequisicoes() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                isManterConexao = false;
                System.out.println("Encerrando conexão por inatividade...");
            }
        }, ParametrosHelper.DELAY); // Encerra a conexão após não receber mais nenhuma solicitação durante determinado tempo

        while (isManterConexao) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String mensagemRecebida = new String(request.getData()).trim();
                int idProcesso = Integer.parseInt(mensagemRecebida.replaceAll("[^0-9]", ""));

                timestamps.putIfAbsent(idProcesso, 0);
                queue.add(idProcesso);

                while (!queue.isEmpty()) {
                    int idProcessoAtual = queue.peek();
                    int timestamp = timestamps.getOrDefault(idProcessoAtual, 0);
                    int entradaRC = Math.max(timestamp, contadorRC) + 1;
                    timestamps.put(idProcessoAtual, entradaRC);

                    String mensagem = "O processo " + idProcessoAtual + " entrou na região crítica";
                    writeAreaCritica(mensagem);
                    System.out.println(mensagem);

                    Thread.sleep(ParametrosHelper.TIME);

                    contadorRC = entradaRC;

                    mensagem = "O processo " + idProcessoAtual + " saiu da região crítica";
                    writeAreaCritica(mensagem);
                    System.out.println(mensagem);

                    buffer = mensagem.getBytes();
                    DatagramPacket resposta = new DatagramPacket(buffer, buffer.length, group, ParametrosHelper.PORTA);
                    socket.send(resposta);

                    queue.poll();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }

    /**
     * Escreve no arquivo o momento em que o processo entrou na região crítica 
     * E o momento em que o processo saiu da região crítica
     * @param mensagem
     */
    private void writeAreaCritica(String mensagem) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("regiao_critica.txt", true))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String momento = dateFormat.format(new Date());
            writer.write(momento + " - " + mensagem);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            System.out.println("Servidor Criado");
            server.processarRequisicoes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
