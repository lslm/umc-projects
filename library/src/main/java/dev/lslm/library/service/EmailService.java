package dev.lslm.library.service;

import org.springframework.stereotype.Service;

/** Simulacao da integracao de e-mail: apenas imprime que a integracao foi chamada. */
@Service
public class EmailService {

    public void enviar(String destinatario, String assunto, String corpo) {
        System.out.println("[EMAIL] Integração de e-mail chamada -> para=" + destinatario
                + " | assunto=" + assunto + " | corpo=" + corpo);
    }
}
