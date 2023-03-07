package com.ruslanburduzhan.catsbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruslanburduzhan.catsbot.entity.Cat;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class CatService {
    public File getCatImage(String cmd) {
        if (cmd == null) {
            cmd = "";
        }
        String urlApi = "https://cataas.com/cat" + cmd;
        if (cmd.contains("?"))
            urlApi += "&json=true";
        else
            urlApi += "?json=true";
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(urlApi)).build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            Cat cat = mapper.readValue(response.body(), Cat.class);

            URL urlImage = new URL("https://cataas.com" + cat.getUrl());
            if (cmd.equals("")) {
                BufferedImage img = ImageIO.read(urlImage);
                File file = new File("downloaded.jpg");
                ImageIO.write(img, "jpg", file);
                return file;
            } else {
                File file = new File("downloaded.gif");

                try (InputStream inputStream = urlImage.openStream();
                     OutputStream outputStream = new FileOutputStream(file)) {
                    IOUtils.copy(inputStream, outputStream);
                }
                return file;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
