package de.muenchen.allg.itd51.wollmux.document;

import java.nio.ByteBuffer;

import com.sun.star.io.IOException;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XSeekable;

/**
 * Wrapper für ByteBuffer zur Benutzung mit UNO.
 */
public class ByteBufferInputStream implements XInputStream, XSeekable
{

  private ByteBuffer buffer;

  public ByteBufferInputStream(ByteBuffer buffer)
  {
    this.buffer = buffer;
  }

  @Override
  public int available() throws IOException
  {
    return buffer.remaining();
  }

  @Override
  public void closeInput() throws IOException
  {
    buffer = null;
  }

  @Override
  public int readBytes(byte[][] data, int len) throws IOException
  {
    int n = Math.min(len, buffer.remaining());
    if (n > 0)
    {
      data[0] = new byte[n];
      buffer.get(data[0], 0, n);
    }
    return n;
  }

  @Override
  public int readSomeBytes(byte[][] data, int len) throws IOException
  {
    return readBytes(data, len);
  }

  @Override
  public void skipBytes(int n) throws IOException
  {
    buffer.position(buffer.position() + n);
  }

  @Override
  public long getLength() throws IOException
  {
    return buffer.capacity();
  }

  @Override
  public long getPosition() throws IOException
  {
    return buffer.position();
  }

  @Override
  public void seek(long pos) throws IOException
  {
    buffer.position((int) pos);
  }
}