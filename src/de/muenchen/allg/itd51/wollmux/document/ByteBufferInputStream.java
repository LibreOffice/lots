package de.muenchen.allg.itd51.wollmux.document;

import java.nio.ByteBuffer;

import com.sun.star.io.BufferSizeExceededException;
import com.sun.star.io.IOException;
import com.sun.star.io.NotConnectedException;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XSeekable;
import com.sun.star.lang.IllegalArgumentException;

public class ByteBufferInputStream implements XInputStream, XSeekable
{

  private ByteBuffer buffer;

  public ByteBufferInputStream(ByteBuffer buffer)
  {
    this.buffer = buffer;
  }

  @Override
  public int available()
      throws NotConnectedException, com.sun.star.io.IOException
  {
    return buffer.remaining();
  }

  @Override
  public void closeInput()
      throws NotConnectedException, com.sun.star.io.IOException
  {
    buffer = null;
  }

  @Override
  public int readBytes(byte[][] data, int len) throws NotConnectedException,
      BufferSizeExceededException, com.sun.star.io.IOException
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
  public int readSomeBytes(byte[][] data, int len)
      throws NotConnectedException, BufferSizeExceededException,
      com.sun.star.io.IOException
  {
    return readBytes(data, len);
  }

  @Override
  public void skipBytes(int n) throws NotConnectedException,
      BufferSizeExceededException, com.sun.star.io.IOException
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
  public void seek(long pos) throws IllegalArgumentException, IOException
  {
    buffer.position((int) pos);
  }
}