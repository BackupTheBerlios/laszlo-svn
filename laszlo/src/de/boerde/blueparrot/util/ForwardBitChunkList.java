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
 * ChunkList.java
 *
 * Created on 10. Mai 2004, 20:17
 */

package de.boerde.blueparrot.util;

import java.io.*;

/**
 *
 * @author  roland
 */
public class ForwardBitChunkList implements Serializable
{
	private Chunk firstChunk;
	private Chunk currentChunk;

	/** Creates a new instance of ChunkList */
	public ForwardBitChunkList()
	{
	}

	public ForwardBitChunkList (int from, int to)
	{
		if (from < 0)
			throw new IllegalArgumentException ("Range cannot be negative");
		if (to <= from)
			throw new IllegalArgumentException ("To is not greater than From");

		firstChunk = new Chunk (from, to-1);	// similar to BitSets: from is inclusive, to is exclusive
		currentChunk = firstChunk;
	}

	public boolean isEmpty()
	{
		return firstChunk == null;
	}

	public boolean get (int index)
	{
		Chunk prec = findChunkBefore (index);
		Chunk c = (prec != null) ? prec.nextChunk : firstChunk;
		if (c != null)
		{
			currentChunk = prec;
			return (index >= c.from) && (index <= c.to);
		}
		else
		{
			currentChunk = prec;
			return false;
		}
	}

	public void clear (int index)
	{
		Chunk prec = findChunkBefore (index);
		Chunk c = (prec != null) ? prec.nextChunk : firstChunk;
		if (c != null)
		{
			if (index == c.from)
			{
				if (index == c.to)
				{
					if (prec == null)
					{
						firstChunk = c.nextChunk;
						c = null;
					}
					else
					{
						prec.nextChunk = c.nextChunk;
						c = prec;
					}
				}
				else
					c.from++;
			}
			else if (index == c.to)
				c.to--;
			else if ((index > c.from) && (index < c.to))
			{
				Chunk newChunk = new Chunk (index+1, c.to);
				newChunk.nextChunk = c.nextChunk;
				c.to = index-1;
				c.nextChunk = newChunk;
			}

			currentChunk = prec;
		}
	}

	public void set (int index)
	{
		Chunk c = findChunkBefore (index);
		if (c == null)
		{
			if ((firstChunk != null) && (firstChunk.to == index-1))
			{
				currentChunk.to = index;
			}
			else if ((firstChunk == null) || (index < firstChunk.from))
			{
				c = new Chunk (index, index);
				c.nextChunk = firstChunk;
				firstChunk = c;
			}
		}
		else
		{
			Chunk c1 = c.nextChunk;
			if (c1 != null)
			{
				if (index == c.to+1)
				{
					if (index == c1.from-1)
					{
						c.to = c1.to;
						c.nextChunk = c1.nextChunk;
					}
					else
					{
						c.to = index;
					}
				}
				else if (index == c1.from-1)
				{
					c1.from = index;
				}
				else if ((index > c.to) && (index < c1.from))
				{
					Chunk newChunk = new Chunk (index, index);
					newChunk.nextChunk = c1;
					c.nextChunk = newChunk;
				}
			}
			else
			{
				if (index == c.to+1)
				{
					c.to = index;
				}
				else if (index > c.to)
				{
					Chunk newChunk = new Chunk (index, index);
					c.nextChunk = newChunk;
				}
			}
		}
		currentChunk = c;
	}

	public int nextSetBit (int firstCandidate)
	{
		int result;
		Chunk c = findChunk (firstCandidate);
		if (c != null)
		{
			if (firstCandidate < c.from)
				result = c.from;
			else if (firstCandidate <= c.to)
				result = firstCandidate;
			else
			{
				c = c.nextChunk;
				if (c != null)
					result = c.from;
				else
					result = -1;
			}
		}
		else
		{
			if ((firstChunk != null) && (firstCandidate < firstChunk.from))
				result = firstChunk.from;
			else
				result = -1;
		}
		currentChunk = c;
		return result;
	}

	public int nextClearBit (int firstCandidate)
	{
		int result;
		Chunk c = findChunk (firstCandidate);
		if (c != null)
		{
			if ((firstCandidate >= c.from) && (firstCandidate <= c.to))
				result = c.to+1;
			else
			{
				result = firstCandidate;
			}
		}
		else
		{
			result = firstCandidate;
		}
		currentChunk = c;
		return result;
	}

	public int length()
	{
		Chunk c = (currentChunk != null) ? currentChunk : firstChunk;
		if (c != null)
		{
			while (c.nextChunk != null)
			{
				c = c.nextChunk;
			}
			currentChunk = c;
			return c.to+1;
		}
		else
			return 0;
	}

	public int cardinality()
	{
		Chunk c = firstChunk;
		int result = 0;
		while (c != null)
		{
			result += (c.to - c.from +1);
			c = c.nextChunk;
		}
		return result;
	}

	public boolean equals (Object o)
	{
		if (o == null)
			return false;
		if (!(o instanceof ForwardBitChunkList))
			return false;

		ForwardBitChunkList other = (ForwardBitChunkList) o;
		Chunk myChunk = firstChunk;
		Chunk otherChunk = other.firstChunk;
		while ((myChunk != null) && (otherChunk != null))
		{
			if ((myChunk.from != otherChunk.from) || (myChunk.to != otherChunk.to))
			{
				return false;
			}
			myChunk = myChunk.nextChunk;
			otherChunk = otherChunk.nextChunk;
		}
		return ((myChunk == null) && (otherChunk == null));
	}

	private Chunk findChunk (int index)
	{
		Chunk c = currentChunk;
		if (((c != null) && (index < c.from)) || (c == null))
			c = firstChunk;
		while ((c != null) && (index > c.to))
		{
			c = c.nextChunk;
		}
		return c;
	}

	private Chunk findChunkBefore (int index)
	{
		Chunk pre = null;
		Chunk c = currentChunk;
		if (((c != null) && ((index < c.from) || (index > c.to))) || (c == null))
			c = firstChunk;
		while ((c != null) && (index > c.to))
		{
			pre = c;
			c = c.nextChunk;
		}
		return pre;
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		Chunk iter = firstChunk;
		while (iter != null)
		{
			buf.append (iter);
			iter = iter.nextChunk;
			if (iter != null)
				buf.append (',');
		}
		return buf.toString();
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		Chunk iter = firstChunk;
		while (iter != null)
		{
			out.writeInt (iter.from);
			out.writeInt (iter.to);
			iter = iter.nextChunk;
		}
		out.writeInt (-1);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		firstChunk = null;
		currentChunk = null;
		Chunk oldc = null;
		int from;
		do
		{
			from = in.readInt();
			if (from < 0)
				break;

			int to = in.readInt();
			Chunk c = new Chunk (from, to);
			if (firstChunk == null)
			{
				firstChunk = c;
			}
			if (oldc != null)
			{
				oldc.nextChunk = c;
			}
			oldc = c;
		}
		while (true);
	}

	private final class Chunk implements Serializable
	{
		protected int from;
		protected int to;
		protected Chunk nextChunk;

		protected Chunk()
		{
		}

		protected Chunk (int from, int to)
		{
			this.from = from;
			this.to = to;
		}

		public String toString()
		{
			if (from != to)
			{
				return from + "-" + to;
			}
			else
			{
				return String.valueOf (from);
			}
		}
	}

	public static void main (String[] args) throws Exception
	{
		ForwardBitChunkList c = new ForwardBitChunkList (3, 7);
		System.out.println (c);
		c.set (10);
		System.out.println (c);
		c.clear (10);
		System.out.println (c);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream (bout);
		out.writeObject (c);
		out.close();
		byte[] bytes = bout.toByteArray();
		ObjectInputStream in = new ObjectInputStream (new ByteArrayInputStream (bytes));
		ForwardBitChunkList bc = (ForwardBitChunkList) in.readObject();
		in.close();
		System.out.println (bc);
	}
}
