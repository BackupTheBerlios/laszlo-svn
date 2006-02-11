/*
Laszlo, a reception software for a satellite-based push service.
Copyright (C) 2004-2006  Roland Fulde

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
MA 02110-1301, USA.

Project home page: http://laszlo.berlios.de/
*/


/*
 * HttpInputStream.java
 *
 * Created on 17. Oktober 2004, 12:55
 */

package de.boerde.blueparrot.io;

import java.io.*;

/**
 *
 * @author  roland
 */
public class LineReadableInputStream extends InputStream
{
	private InputStream parentStream;

	/** Creates a new instance of HttpInputStream */
	public LineReadableInputStream (InputStream parentStream)
	{
		this.parentStream = parentStream;
	}

	public void close() throws IOException
	{
		parentStream.close();
	}

	public void mark (int readlimit)
	{
		parentStream.mark (readlimit);
	}

	public void reset() throws IOException
	{
		parentStream.reset();
	}

	public boolean markSupported()
	{
		return false;
	}

	public int read() throws IOException
	{
		if (prereadLen > 0)
		{
			byte b = prereadBytes [prereadStart];
			prereadStart++;
			prereadLen--;
			return (int) (b & 0xff);
		}
		else
		{
			return parentStream.read();
		}
	}

	public int read (byte[] b) throws IOException
	{
		return read (b, 0, b.length);
	}

	public int read (byte[] b, int off, int len) throws IOException
	{
		if (prereadLen > 0)
		{
			if (prereadLen < len)
			{
				System.arraycopy (prereadBytes, prereadStart, b, off, prereadLen);
				int actual = prereadLen;
				prereadLen = 0;
				return actual;
			}
			else
			{
				System.arraycopy (prereadBytes, prereadStart, b, off, len);
				prereadStart += len;
				prereadLen -= len;
				return len;
			}
		}
		else
		{
			return parentStream.read (b, off, len);
		}
	}

	public long skip (long n) throws IOException
	{
		if (n <= prereadLen)
		{
			prereadStart += n;
			prereadLen -= n;
			return n;
		}
		else
		{
			long actual = parentStream.skip (n-prereadLen);
			prereadStart = 0;
			prereadLen = 0;
			return actual;
		}
	}

	public int available() throws IOException
	{
		if (prereadLen > 0)
			return prereadLen;
		else
			return parentStream.available();
	}

	public String readLine() throws IOException
	{
		int lineSeparatorMatchLength = 0;
		byte[] bytesForString = null;
	stringReading:
		while (true)
		{
			if (prereadLen <= 0)
			{
				prereadStart = 0;
				prereadLen = parentStream.read (prereadBytes);
				if (prereadLen < 0)
				{
					break;
				}
			}
			for (int p=0; p<prereadLen; p++)
			{
				if (prereadBytes [prereadStart+p] == lineSeparator [lineSeparatorMatchLength])
				{
					lineSeparatorMatchLength++;
					if (lineSeparatorMatchLength == lineSeparator.length)
					{
						p++;
						bytesForString = extendByteArray (bytesForString, prereadBytes, prereadStart, p);
						prereadStart += p;
						prereadLen -= p;
						break stringReading;
					}
				}
				else
				{
					lineSeparatorMatchLength = 0;
				}
			}
			bytesForString = extendByteArray (bytesForString, prereadBytes, prereadStart, prereadLen);
			prereadLen = 0;
		}

		if (bytesForString == null)
			return null;

		return new String (bytesForString, 0, bytesForString.length - lineSeparator.length, "US-ASCII");
	}

	public void pushback (byte b) throws IOException
	{
		if (prereadLen > 0)
		{
			if (prereadStart > 0)
			{
				prereadStart--;
				prereadBytes [prereadStart] = b;
			}
			else if (prereadStart == 0 && prereadLen < prereadBytes.length)
			{
				System.arraycopy (prereadBytes, 0, prereadBytes, 1, prereadLen);
				prereadLen++;
				prereadBytes [prereadStart] = b;
			}
			else
			{
				byte[] temp = new byte [prereadBytes.length + PREREAD_BUFFER];
				System.arraycopy (prereadBytes, 0, temp, PREREAD_BUFFER,  prereadBytes.length);
				prereadStart = PREREAD_BUFFER-1;
				temp [prereadStart] = b;
				prereadBytes = temp;
			}
		}
		else
		{
			prereadStart = prereadBytes.length -1;
			prereadLen = 1;
			prereadBytes [prereadStart] = b;
		}
	}

	public void pushbackAfterPreread (byte b) throws IOException
	{
		if (prereadLen > 0)
		{
			if (prereadStart >= 0 && prereadStart+prereadLen < prereadBytes.length)
			{
				prereadBytes [prereadStart+prereadLen] = b;
				prereadLen++;
			}
			else if (prereadStart > 0)
			{
				System.arraycopy (prereadBytes, prereadStart, prereadBytes, prereadStart-1,  prereadLen);
				prereadStart--;
				prereadBytes [prereadStart+prereadLen] = b;
				prereadLen++;
			}
			else
			{
				byte[] temp = new byte [prereadBytes.length + PREREAD_BUFFER];
				System.arraycopy (prereadBytes, 0, temp, 0,  prereadBytes.length);
				prereadStart = 0;
				temp [prereadLen] = b;
				prereadLen++;
				prereadBytes = temp;
			}
		}
		else
		{
			prereadStart = prereadBytes.length -1;
			prereadLen = 1;
			prereadBytes [prereadStart] = b;
		}
	}

	private byte[] extendByteArray (final byte[] oldBytes, byte[] newBytes, int newBytesStart, int newBytesLen)
	{
		if (oldBytes == null)
		{
			byte[] temp = new byte [newBytesLen];
			System.arraycopy (newBytes, newBytesStart, temp, 0, newBytesLen);
			return temp;
		}
		else
		{
			byte[] temp = new byte [oldBytes.length + newBytesLen];
			System.arraycopy (oldBytes, 0, temp, 0, oldBytes.length);
			System.arraycopy (newBytes, newBytesStart, temp, oldBytes.length, newBytesLen);
			return temp;
		}
	}

	private static int PREREAD_BUFFER = 2048;
	private byte[] prereadBytes = new byte [PREREAD_BUFFER];
	private int prereadStart = 0;
	private int prereadLen = 0;

	private byte[] lineSeparator = { 0x0d, 0x0a };
}
