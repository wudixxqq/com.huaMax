import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class LocationMaxCodeGenerator {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java LocationMaxCodeGenerator.java <private-key.pem> <days 1-10> [note]");
            System.exit(2);
        }

        Path privateKeyPath = Path.of(args[0]);
        int days = Integer.parseInt(args[1]);
        String note = args.length >= 3 ? args[2].trim() : "";

        if (days < 1 || days > 10) {
            throw new IllegalArgumentException("Days must be between 1 and 10.");
        }

        PrivateKey privateKey = loadPrivateKey(privateKeyPath);
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + days * 24L * 60L * 60L;

        String payloadJson = "{\"sub\":\"LocationMax\",\"iat\":" + issuedAt + ",\"exp\":" + expiresAt
            + (note.isEmpty() ? "" : ",\"note\":\"" + escapeJson(note) + "\"")
            + "}";
        String payloadPart = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(payloadPart.getBytes(StandardCharsets.US_ASCII));
        String signaturePart = base64Url(signer.sign());

        System.out.println("LM1." + payloadPart + "." + signaturePart);
        System.out.println();
        System.out.println("Expires UTC: " + DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochSecond(expiresAt)));
    }

    private static PrivateKey loadPrivateKey(Path path) throws Exception {
        String pem = Files.readString(path, StandardCharsets.UTF_8)
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\f", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
