package ca.kieve.dynamicqrbot.controller;

import ca.kieve.dynamicqrbot.model.QrMapping;
import ca.kieve.dynamicqrbot.service.BotConfigService;
import ca.kieve.dynamicqrbot.service.QrImageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    private final BotConfigService m_configService;
    private final QrImageService m_qrImageService;

    public HomeController(BotConfigService configService, QrImageService qrImageService) {
        m_configService = configService;
        m_qrImageService = qrImageService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<QrMapping> mappings = m_configService.getMappings();
        Map<String, String> imageFiles = m_qrImageService.getAllCachedImages();
        model.addAttribute("mappings", mappings);
        model.addAttribute("imageFiles", imageFiles);
        return "home";
    }

    @GetMapping("/qr-images/{filename}")
    @ResponseBody
    public ResponseEntity<String> serveQrImage(@PathVariable String filename) {
        String content = m_qrImageService.getImageContent(filename);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(content);
    }
}
