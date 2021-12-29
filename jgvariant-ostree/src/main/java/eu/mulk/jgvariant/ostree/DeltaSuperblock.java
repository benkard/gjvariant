package eu.mulk.jgvariant.ostree;

import eu.mulk.jgvariant.core.Decoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * A static delta.
 *
 * <p>Reference: {@code ostree-repo-static-delta-private.h#OSTREE_STATIC_DELTA_SUPERBLOCK_FORMAT}
 *
 * @param metadata arbitrary user-supplied metadata.
 * @param timestamp UNIX epoch seconds of when the commit was done.
 * @param fromChecksum a (possibly {@link Checksum#isEmpty()}) reference to the starting commit.
 * @param toChecksum a (non-{@link Checksum#isEmpty()}) reference to the end commit.
 * @param commit the commit metadata of the end commit.
 * @param dependencies a list of other {@link DeltaSuperblock}s that need to be applied before this
 *     one.
 * @param entries a list of metadata on the {@link DeltaPartPayload}s that make up the delta.
 * @param fallbacks a list of objects included in the delta as plain files that have to be fetched
 *     separately.
 */
public record DeltaSuperblock(
    Metadata metadata,
    long timestamp,
    Checksum fromChecksum,
    Checksum toChecksum,
    Commit commit,
    List<DeltaName> dependencies,
    List<DeltaMetaEntry> entries,
    List<DeltaFallback> fallbacks) {

  /**
   * A specifier for another static delta.
   *
   * <p>Used to specify {@link DeltaSuperblock#dependencies()}.
   *
   * @param fromChecksum the {@link DeltaSuperblock#fromChecksum()} of the referenced delta.
   * @param toChecksum the {@link DeltaSuperblock#toChecksum()} of the referenced delta.
   */
  public record DeltaName(Checksum fromChecksum, Checksum toChecksum) {

    private static final Decoder<DeltaName> DECODER =
        Decoder.ofStructure(DeltaName.class, Checksum.decoder(), Checksum.decoder());

    /**
     * Acquires a {@link Decoder} for the enclosing type.
     *
     * @return a possibly shared {@link Decoder}.
     */
    public static Decoder<DeltaName> decoder() {
      return DECODER;
    }
  }

  private static final Decoder<DeltaSuperblock> DECODER =
      Decoder.ofStructure(
          DeltaSuperblock.class,
          Metadata.decoder(),
          Decoder.ofLong().withByteOrder(ByteOrder.BIG_ENDIAN),
          Checksum.decoder(),
          Checksum.decoder(),
          Commit.decoder(),
          Decoder.ofByteArray().map(DeltaSuperblock::parseDeltaNameList),
          Decoder.ofArray(DeltaMetaEntry.decoder()),
          Decoder.ofArray(DeltaFallback.decoder()));

  private static List<DeltaName> parseDeltaNameList(byte[] bytes) {
    var byteBuffer = ByteBuffer.wrap(bytes);
    List<DeltaName> deltaNames = new ArrayList<>();

    while (byteBuffer.hasRemaining()) {
      var fromChecksum = Checksum.readFrom(byteBuffer);
      var toChecksum = Checksum.readFrom(byteBuffer);
      deltaNames.add(new DeltaName(fromChecksum, toChecksum));
    }

    return deltaNames;
  }

  /**
   * Acquires a {@link Decoder} for the enclosing type.
   *
   * @return a possibly shared {@link Decoder}.
   */
  public static Decoder<DeltaSuperblock> decoder() {
    return DECODER;
  }
}
