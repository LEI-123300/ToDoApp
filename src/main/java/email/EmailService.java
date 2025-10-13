package email;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "tiagofranca6@gmail.com";
    private static final String PASSWORD = "hjjvemeuweroqots";

    /**
     * Envia notificação quando uma tarefa é criada
     */
    public static void sendTaskCreatedNotification(String userEmail, String taskDescription) {
        String subject = "Nova Tarefa Criada: " + taskDescription;
        String body = "Olá!\n\n" +
                "Foi criada uma nova tarefa: " + taskDescription + "\n\n" +
                "Boa sorte com a conclusão!\n\n" +
                "Equipa TO-DO App";
        sendEmail(userEmail, subject, body);
    }

    /**
     * Método interno para enviar email
     */
    private static void sendEmail(String to, String subject, String body) {
        // Configurar propriedades do SMTP
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        // Criar sessão com autenticação
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            // Criar mensagem
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(body);

            // Enviar email
            Transport.send(message);
            System.out.println("✅ Email enviado com sucesso para: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Erro ao enviar email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
