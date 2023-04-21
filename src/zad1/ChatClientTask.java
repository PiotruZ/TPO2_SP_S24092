/**
 *
 *  @author Szczęsny Piotr S24092
 *
 */

package zad1;


import java.util.List;
import java.util.concurrent.ExecutionException;

public class ChatClientTask implements Runnable {
    private ChatClient client;
    private List<String> messages;
    private int waitTime;

    public ChatClientTask(ChatClient client, List<String> messages, int waitTime) {
        this.client = client;
        this.messages = messages;
        this.waitTime = waitTime;
    }

    public ChatClient getClient() {
        return client;
    }

    public ChatClientTask get() throws InterruptedException, ExecutionException {
        return this;
    }

    public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
        return new ChatClientTask(c, msgs, wait);
    }



    @Override
    public void run() {
        client.login();
        if (waitTime != 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (String message : messages) {
            client.send(message);
            if (waitTime != 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        client.logout();
        if (waitTime != 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}



