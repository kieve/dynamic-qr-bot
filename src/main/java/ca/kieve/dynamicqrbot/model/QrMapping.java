package ca.kieve.dynamicqrbot.model;

public record QrMapping(
        String nickname,
        String staticPath,
        String destinationUrl
) {
    public QrMapping withDestinationUrl(String newUrl) {
        return new QrMapping(nickname, staticPath, newUrl);
    }
}
