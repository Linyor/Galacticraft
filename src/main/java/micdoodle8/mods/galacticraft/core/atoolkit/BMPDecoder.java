/*
 * Decodes a BMP image from an <tt>InputStream</tt> to a <tt>BufferedImage</tt>
 *  
 * @author Ian McDonagh
 */

package micdoodle8.mods.galacticraft.core.atoolkit;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * Decodes images in BMP format.
 * 
 * @author Ian McDonagh
 */
public class BMPDecoder
{

	private BufferedImage img;
	private BMPInfoHeader infoHeader;

	/**
	 * Creates a new instance of BMPDecoder and reads the BMP data from the
	 * source.
	 * 
	 * @param in
	 *            the source <tt>InputStream<tt> from which to read the BMP data
	 * @throws java.io.IOException
	 *             if an error occurs
	 */
	public BMPDecoder(java.io.InputStream in) throws IOException
	{
		LittleEndianInputStream lis = new LittleEndianInputStream(in);

		/* header [14] */

		//signature "BM" [2]
		byte[] bsignature = new byte[2];
		lis.read(bsignature);
		String signature = new String(bsignature, "UTF-8");

		if (!signature.equals("BM"))
		{
			lis.close();
			throw new IOException("Invalid signature '" + signature + "' for BMP format");
		}

		//file size [4]
		int fileSize = lis.readIntLE();

		//reserved = 0 [4]
		int reserved = lis.readIntLE();

		//DataOffset [4] file offset to raster data
		int dataOffset = lis.readIntLE();

		/* info header [40] */

		this.infoHeader = BMPDecoder.readInfoHeader(lis);

		/* Color table and Raster data */

		this.img = BMPDecoder.read(this.infoHeader, lis);
	}

	/**
	 * Retrieves a bit from the lowest order byte of the given integer.
	 * 
	 * @param bits
	 *            the source integer, treated as an unsigned byte
	 * @param index
	 *            the index of the bit to retrieve, which must be in the range
	 *            <tt>0..7</tt>.
	 * @return the bit at the specified index, which will be either <tt>0</tt>
	 *         or <tt>1</tt>.
	 */
	private static int getBit(int bits, int index)
	{
		return bits >> 7 - index & 1;
	}

	/**
	 * Retrieves a nibble (4 bits) from the lowest order byte of the given
	 * integer.
	 * 
	 * @param nibbles
	 *            the source integer, treated as an unsigned byte
	 * @param index
	 *            the index of the nibble to retrieve, which must be in the
	 *            range <tt>0..1</tt>.
	 * @return the nibble at the specified index, as an unsigned byte.
	 */
	private static int getNibble(int nibbles, int index)
	{
		return nibbles >> 4 * (1 - index) & 0xF;
	}

	/**
	 * The <tt>InfoHeader</tt> structure, which provides information about the
	 * BMP data.
	 * 
	 * @return the <tt>InfoHeader</tt> structure that was read from the source
	 *         data when this <tt>BMPDecoder</tt> was created.
	 */
	public BMPInfoHeader getInfoHeader()
	{
		return this.infoHeader;
	}

	/**
	 * The decoded image read from the source input.
	 * 
	 * @return the <tt>BufferedImage</tt> representing the BMP image.
	 */
	public BufferedImage getBufferedImage()
	{
		return this.img;
	}

	private static void getColorTable(BMPColorEntry[] colorTable, byte[] ar, byte[] ag, byte[] ab)
	{
		for (int i = 0; i < colorTable.length; i++)
		{
			ar[i] = (byte) colorTable[i].bRed;
			ag[i] = (byte) colorTable[i].bGreen;
			ab[i] = (byte) colorTable[i].bBlue;
		}
	}

	/**
	 * Reads the BMP info header structure from the given <tt>InputStream</tt>.
	 * 
	 * @param lis
	 *            the <tt>InputStream</tt> to read
	 * @return the <tt>InfoHeader</tt> structure
	 * @throws java.io.IOException
	 *             if an error occurred
	 */
	public static BMPInfoHeader readInfoHeader(LittleEndianInputStream lis) throws IOException
	{
		BMPInfoHeader infoHeader = new BMPInfoHeader(lis);
		return infoHeader;
	}

	/**
	 * @since 0.6
	 */
	public static BMPInfoHeader readInfoHeader(LittleEndianInputStream lis, int infoSize) throws IOException
	{
		BMPInfoHeader infoHeader = new BMPInfoHeader(lis, infoSize);
		return infoHeader;
	}

	/**
	 * Reads the BMP data from the given <tt>InputStream</tt> using the
	 * information contained in the <tt>InfoHeader</tt>.
	 * 
	 * @param lis
	 *            the source input
	 * @param infoHeader
	 *            an <tt>InfoHeader</tt> that was read by a call to
	 *            {@link #readInfoHeader(LittleEndianInputStream)
	 *            readInfoHeader()}.
	 * @return the decoded image read from the source input
	 * @throws java.io.IOException
	 *             if an error occurs
	 */
	public static BufferedImage read(BMPInfoHeader infoHeader, LittleEndianInputStream lis) throws IOException
	{
		BufferedImage img = null;

		/* Color table (palette) */

		BMPColorEntry[] colorTable = null;

		//color table is only present for 1, 4 or 8 bit (indexed) images
		if (infoHeader.sBitCount <= 8)
		{
			colorTable = BMPDecoder.readColorTable(infoHeader, lis);
		}

		img = BMPDecoder.read(infoHeader, lis, colorTable);

		return img;
	}

	/**
	 * Reads the BMP data from the given <tt>InputStream</tt> using the
	 * information contained in the <tt>InfoHeader</tt>.
	 * 
	 * @param colorTable
	 *            <tt>ColorEntry</tt> array containing palette
	 * @param infoHeader
	 *            an <tt>InfoHeader</tt> that was read by a call to
	 *            {@link #readInfoHeader(LittleEndianInputStream)
	 *            readInfoHeader()}.
	 * @param lis
	 *            the source input
	 * @return the decoded image read from the source input
	 * @throws java.io.IOException
	 *             if any error occurs
	 */
	public static BufferedImage read(BMPInfoHeader infoHeader, LittleEndianInputStream lis, BMPColorEntry[] colorTable) throws IOException
	{

		BufferedImage img = null;

		//1-bit (monochrome) uncompressed
		if (infoHeader.sBitCount == 1 && infoHeader.iCompression == BMPConstants.BI_RGB)
		{

			img = BMPDecoder.read1(infoHeader, lis, colorTable);

		}
		//4-bit uncompressed
		else if (infoHeader.sBitCount == 4 && infoHeader.iCompression == BMPConstants.BI_RGB)
		{

			img = BMPDecoder.read4(infoHeader, lis, colorTable);

		}
		//8-bit uncompressed
		else if (infoHeader.sBitCount == 8 && infoHeader.iCompression == BMPConstants.BI_RGB)
		{

			img = BMPDecoder.read8(infoHeader, lis, colorTable);

		}
		//24-bit uncompressed
		else if (infoHeader.sBitCount == 24 && infoHeader.iCompression == BMPConstants.BI_RGB)
		{

			img = BMPDecoder.read24(infoHeader, lis);

		}
		//32bit uncompressed
		else if (infoHeader.sBitCount == 32 && infoHeader.iCompression == BMPConstants.BI_RGB)
		{

			img = BMPDecoder.read32(infoHeader, lis);

		}
		else
		{
			throw new IOException("Unrecognized bitmap format: bit count=" + infoHeader.sBitCount + ", compression=" + infoHeader.iCompression);
		}

		return img;
	}

	/**
	 * Reads the <tt>ColorEntry</tt> table from the given <tt>InputStream</tt>
	 * using the information contained in the given <tt>infoHeader</tt>.
	 * 
	 * @param infoHeader
	 *            the <tt>InfoHeader</tt> structure, which was read using
	 *            {@link #readInfoHeader(LittleEndianInputStream)
	 *            readInfoHeader()}
	 * @param lis
	 *            the <tt>InputStream</tt> to read
	 * @throws java.io.IOException
	 *             if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static BMPColorEntry[] readColorTable(BMPInfoHeader infoHeader, LittleEndianInputStream lis) throws IOException
	{
		BMPColorEntry[] colorTable = new BMPColorEntry[infoHeader.iNumColors];
		for (int i = 0; i < infoHeader.iNumColors; i++)
		{
			BMPColorEntry ce = new BMPColorEntry(lis);
			colorTable[i] = ce;
		}
		return colorTable;
	}

	/**
	 * Reads 1-bit uncompressed bitmap raster data, which may be monochrome
	 * depending on the palette entries in <tt>colorTable</tt>.
	 * 
	 * @param infoHeader
	 *            the <tt>InfoHeader</tt> structure, which was read using
	 *            {@link #readInfoHeader(LittleEndianInputStream)
	 *            readInfoHeader()}
	 * @param lis
	 *            the source input
	 * @param colorTable
	 *            <tt>ColorEntry</tt> array specifying the palette, which must
	 *            not be <tt>null</tt>.
	 * @throws java.io.IOException
	 *             if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static BufferedImage read1(BMPInfoHeader infoHeader, LittleEndianInputStream lis, BMPColorEntry[] colorTable) throws IOException
	{
		//1 bit per pixel or 8 pixels per byte
		//each pixel specifies the palette index

		byte[] ar = new byte[colorTable.length];
		byte[] ag = new byte[colorTable.length];
		byte[] ab = new byte[colorTable.length];

		BMPDecoder.getColorTable(colorTable, ar, ag, ab);

		IndexColorModel icm = new IndexColorModel(1, 2, ar, ag, ab);

		// Create indexed image
		BufferedImage img = new BufferedImage(infoHeader.iWidth, infoHeader.iHeight, BufferedImage.TYPE_BYTE_BINARY, icm);
		// We'll use the raster to set samples instead of RGB values.
		// The SampleModel of an indexed image interprets samples as
		// the index of the colour for a pixel, which is perfect for use here.
		WritableRaster raster = img.getRaster();

		//padding    

		int dataBitsPerLine = infoHeader.iWidth;
		int bitsPerLine = dataBitsPerLine;
		if (bitsPerLine % 32 != 0)
		{
			bitsPerLine = (bitsPerLine / 32 + 1) * 32;
		}
		int padBits = bitsPerLine - dataBitsPerLine;
		int padBytes = padBits / 8;

		int bytesPerLine = bitsPerLine / 8;
		int[] line = new int[bytesPerLine];

		for (int y = infoHeader.iHeight - 1; y >= 0; y--)
		{
			for (int i = 0; i < bytesPerLine; i++)
			{
				line[i] = lis.readUnsignedByte();
			}

			for (int x = 0; x < infoHeader.iWidth; x++)
			{
				int i = x / 8;
				int v = line[i];
				int b = x % 8;
				int index = BMPDecoder.getBit(v, b);
				//int rgb = c[index];
				//img.setRGB(x, y, rgb);
				//set the sample (colour index) for the pixel
				raster.setSample(x, y, 0, index);
			}
		}

		return img;
	}

	/**
	 * Reads 4-bit uncompressed bitmap raster data, which is interpreted based
	 * on the colours specified in the palette.
	 * 
	 * @param infoHeader
	 *            the <tt>InfoHeader</tt> structure, which was read using
	 *            {@link #readInfoHeader(LittleEndianInputStream)
	 *            readInfoHeader()}
	 * @param lis
	 *            the source input
	 * @param colorTable
	 *            <tt>ColorEntry</tt> array specifying the palette, which must
	 *            not be <tt>null</tt>.
	 * @throws java.io.IOException
	 *             if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static BufferedImage read4(BMPInfoHeader infoHeader, LittleEndianInputStream lis, BMPColorEntry[] colorTable) throws IOException
	{

		// 2 pixels per byte or 4 bits per pixel.
		// Colour for each pixel specified by the color index in the pallette.

		byte[] ar = new byte[colorTable.length];
		byte[] ag = new byte[colorTable.length];
		byte[] ab = new byte[colorTable.length];

		BMPDecoder.getColorTable(colorTable, ar, ag, ab);

		IndexColorModel icm = new IndexColorModel(4, infoHeader.iNumColors, ar, ag, ab);

		BufferedImage img = new BufferedImage(infoHeader.iWidth, infoHeader.iHeight, BufferedImage.TYPE_BYTE_BINARY, icm);

		WritableRaster raster = img.getRaster();

		//padding
		int bitsPerLine = infoHeader.iWidth * 4;
		if (bitsPerLine % 32 != 0)
		{
			bitsPerLine = (bitsPerLine / 32 + 1) * 32;
		}
		int bytesPerLine = bitsPerLine / 8;

		int[] line = new int[bytesPerLine];

		for (int y = infoHeader.iHeight - 1; y >= 0; y--)
		{
			//scan line
			for (int i = 0; i < bytesPerLine; i++)
			{
				int b = lis.readUnsignedByte();
				line[i] = b;
			}

			//get pixels
			for (int x = 0; x < infoHeader.iWidth; x++)
			{
				//get byte index for line
				int b = x / 2; // 2 pixels per byte
				int i = x % 2;
				int n = line[b];
				int index = BMPDecoder.getNibble(n, i);
				raster.setSample(x, y, 0, index);
			}
		}

		return img;
	}

	/**
	 * Reads 8-bit uncompressed bitmap raster data, which is interpreted based
	 * on the colours specified in the palette.
	 * 
	 * @param infoHeader
	 *            the <tt>InfoHeader</tt> structure, which was read using
	 *            {@link #readInfoHeader(LittleEndianInputStream)
	 *            readInfoHeader()}
	 * @param lis
	 *            the source input
	 * @param colorTable
	 *            <tt>ColorEntry</tt> array specifying the palette, which must
	 *            not be <tt>null</tt>.
	 * @throws java.io.IOException
	 *             if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static BufferedImage read8(BMPInfoHeader infoHeader, LittleEndianInputStream lis, BMPColorEntry[] colorTable) throws IOException
	{
		//1 byte per pixel
		//  color index 1 (index of color in palette)
		//lines padded to nearest 32bits
		//no alpha

		byte[] ar = new byte[colorTable.length];
		byte[] ag = new byte[colorTable.length];
		byte[] ab = new byte[colorTable.length];

		BMPDecoder.getColorTable(colorTable, ar, ag, ab);

		IndexColorModel icm = new IndexColorModel(8, infoHeader.iNumColors, ar, ag, ab);

		BufferedImage img = new BufferedImage(infoHeader.iWidth, infoHeader.iHeight, BufferedImage.TYPE_BYTE_INDEXED, icm);

		WritableRaster raster = img.getRaster();

		/*
		//create color pallette
		int[] c = new int[infoHeader.iNumColors];
		for (int i = 0; i < c.length; i++) {
		  int r = colorTable[i].bRed;
		  int g = colorTable[i].bGreen;
		  int b = colorTable[i].bBlue;
		  c[i] = (r << 16) | (g << 8) | (b);
		}
		 */

		//padding
		int dataPerLine = infoHeader.iWidth;
		int bytesPerLine = dataPerLine;
		if (bytesPerLine % 4 != 0)
		{
			bytesPerLine = (bytesPerLine / 4 + 1) * 4;
		}
		int padBytesPerLine = bytesPerLine - dataPerLine;

		for (int y = infoHeader.iHeight - 1; y >= 0; y--)
		{
			for (int x = 0; x < infoHeader.iWidth; x++)
			{
				int b = lis.readUnsignedByte();
				//int clr = c[b];
				//img.setRGB(x, y, clr);
				//set sample (colour index) for pixel
				raster.setSample(x, y, 0, b);
			}

			lis.skipBytes(padBytesPerLine);
		}

		return img;
	}

	/**
	 * Reads 24-bit uncompressed bitmap raster data.
	 * 
	 * @param lis
	 *            the source input
	 * @param infoHeader
	 *            the <tt>InfoHeader</tt> structure, which was read using
	 *            {@link #readInfoHeader(LittleEndianInputStream)
	 *            readInfoHeader()}
	 * @throws java.io.IOException
	 *             if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static BufferedImage read24(BMPInfoHeader infoHeader, LittleEndianInputStream lis) throws IOException
	{
		//3 bytes per pixel
		//  blue 1
		//  green 1
		//  red 1
		// lines padded to nearest 32 bits
		// no alpha

		BufferedImage img = new BufferedImage(infoHeader.iWidth, infoHeader.iHeight, BufferedImage.TYPE_INT_RGB);

		WritableRaster raster = img.getRaster();

		//padding to nearest 32 bits
		int dataPerLine = infoHeader.iWidth * 3;
		int bytesPerLine = dataPerLine;
		if (bytesPerLine % 4 != 0)
		{
			bytesPerLine = (bytesPerLine / 4 + 1) * 4;
		}
		int padBytesPerLine = bytesPerLine - dataPerLine;

		for (int y = infoHeader.iHeight - 1; y >= 0; y--)
		{
			for (int x = 0; x < infoHeader.iWidth; x++)
			{
				int b = lis.readUnsignedByte();
				int g = lis.readUnsignedByte();
				int r = lis.readUnsignedByte();

				//int c = 0x00000000 | (r << 16) | (g << 8) | (b);
				//System.out.println(x + ","+y+"="+Integer.toHexString(c));
				//img.setRGB(x, y, c);
				raster.setSample(x, y, 0, r);
				raster.setSample(x, y, 1, g);
				raster.setSample(x, y, 2, b);
			}
			lis.skipBytes(padBytesPerLine);
		}

		return img;
	}

	/**
	 * Reads 32-bit uncompressed bitmap raster data, with transparency.
	 * 
	 * @param lis
	 *            the source input
	 * @param infoHeader
	 *            the <tt>InfoHeader</tt> structure, which was read using
	 *            {@link #readInfoHeader(LittleEndianInputStream)
	 *            readInfoHeader()}
	 * @throws java.io.IOException
	 *             if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static BufferedImage read32(BMPInfoHeader infoHeader, LittleEndianInputStream lis) throws IOException
	{
		//4 bytes per pixel
		// blue 1
		// green 1
		// red 1
		// alpha 1
		//No padding since each pixel = 32 bits

		BufferedImage img = new BufferedImage(infoHeader.iWidth, infoHeader.iHeight, BufferedImage.TYPE_INT_ARGB);

		WritableRaster rgb = img.getRaster();
		WritableRaster alpha = img.getAlphaRaster();

		for (int y = infoHeader.iHeight - 1; y >= 0; y--)
		{
			for (int x = 0; x < infoHeader.iWidth; x++)
			{
				int b = lis.readUnsignedByte();
				int g = lis.readUnsignedByte();
				int r = lis.readUnsignedByte();
				int a = lis.readUnsignedByte();
				rgb.setSample(x, y, 0, r);
				rgb.setSample(x, y, 1, g);
				rgb.setSample(x, y, 2, b);
				alpha.setSample(x, y, 0, a);
			}
		}

		return img;
	}

	/**
	 * Reads and decodes BMP data from the source file.
	 * 
	 * @param file
	 *            the source file
	 * @throws java.io.IOException
	 *             if an error occurs
	 * @return the decoded image read from the source file
	 */
	public static BufferedImage read(java.io.File file) throws IOException
	{
		return BMPDecoder.read(new java.io.FileInputStream(file));
	}

	/**
	 * Reads and decodes BMP data from the source input.
	 * 
	 * @param in
	 *            the source input
	 * @throws java.io.IOException
	 *             if an error occurs
	 * @return the decoded image read from the source file
	 */
	public static BufferedImage read(java.io.InputStream in) throws IOException
	{
		BMPDecoder d = new BMPDecoder(in);
		return d.getBufferedImage();
	}
}
