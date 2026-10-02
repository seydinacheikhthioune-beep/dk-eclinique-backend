package com.eclinique.service;

import com.eclinique.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/** Envoi des documents (factures, exports comptables) par e-mail via le serveur SMTP configuré. */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public record PieceJointe(String nom, byte[] contenu, String type) {}

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.host:}")
    private String hote;
    @Value("${app.mail.expediteur:}")
    private String expediteur;
    @Value("${app.mail.nom-expediteur:}")
    private String nomExpediteur;
    @Value("${app.mail.comptable:}")
    private String comptable;

    public boolean estConfigure() {
        return !hote.isBlank() && !expediteur.isBlank() && mailSender.getIfAvailable() != null;
    }

    /** Adresses du comptable configurées par défaut (COMPTABLE_EMAIL). */
    public List<String> destinatairesComptables() {
        return separer(comptable);
    }

    /** Découpe "a@x.sn, b@y.sn; c@z.sn" en adresses distinctes et vérifie leur format. */
    public List<String> separer(String adresses) {
        if (adresses == null || adresses.isBlank()) return List.of();
        List<String> liste = Arrays.stream(adresses.split("[,;\\s]+")).map(String::trim)
                .filter(a -> !a.isEmpty()).distinct().toList();
        liste.stream().filter(a -> !EMAIL.matcher(a).matches()).findFirst().ifPresent(a -> {
            throw new BusinessException("Adresse e-mail invalide : " + a);
        });
        return liste;
    }

    public void envoyer(Collection<String> destinataires, String sujet, String texte, List<PieceJointe> piecesJointes) {
        if (!estConfigure()) {
            throw new BusinessException("L'envoi d'e-mails n'est pas configuré sur le serveur "
                    + "(variables MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD).");
        }
        if (destinataires == null || destinataires.isEmpty()) {
            throw new BusinessException("Aucun destinataire renseigné");
        }
        JavaMailSender sender = mailSender.getObject();
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(expediteur, nomExpediteur.isBlank() ? null : nomExpediteur, "UTF-8"));
            helper.setTo(destinataires.toArray(String[]::new));
            helper.setSubject(sujet);
            helper.setText(texte, false);
            for (PieceJointe pj : piecesJointes) {
                helper.addAttachment(pj.nom(), new ByteArrayResource(pj.contenu()), pj.type());
            }
            sender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new BusinessException("Échec de l'envoi de l'e-mail : " + e.getMessage());
        }
    }
}
