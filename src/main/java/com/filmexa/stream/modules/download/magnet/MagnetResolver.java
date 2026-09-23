package com.filmexa.stream.modules.download.magnet;

/**
 * Turns a movie id into a magnet link.
 *
 * <p>This is the missing link between "the user clicked play" and "start downloading":
 * the frontend only ever knows the TMDB id, and it must never supply a magnet itself -
 * that would make the backend download whatever anyone asks it to.
 */
public interface MagnetResolver {

    /**
     * @param movieId the TMDB id of the movie to find a torrent for
     * @return a magnet URI
     * @throws com.filmexa.stream.common.exception.NotFoundException when no torrent exists
     */
    String resolve(Long movieId);
}
