package embryo.gui;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class TextFileAccess 
{
	public static void copyFile( final File inputFile, final File outputFile ) throws IOException
	{
		InputStream input = null;
		OutputStream output = null;
		
		try
		{
			input = new FileInputStream( inputFile );
			output = new FileOutputStream( outputFile );

			final byte[] buf = new byte[ 65536 ];
			int bytesRead;
			while ( ( bytesRead = input.read( buf ) ) > 0 )
				output.write( buf, 0, bytesRead );

		}
		finally
		{
			if ( input != null )
				input.close();
			if ( output != null )
				output.close();
		}
	}

	public static BufferedReader openFileRead(final File file)
	{
		BufferedReader inputFile;
		try
		{
			inputFile = new BufferedReader(new FileReader(file));
		}
		catch (IOException e)
		{
			System.out.println("TextFileAccess.openFileRead(): " + e);
			inputFile = null;
		}
		return (inputFile);
	}

	public static BufferedReader openFileRead(final String fileName)
	{
		BufferedReader inputFile;
		try
		{
			inputFile = new BufferedReader(new FileReader(fileName));
		}
		catch (IOException e)
		{
			System.out.println("TextFileAccess.openFileRead(): " + e);
			inputFile = null;
		}
		return (inputFile);
	}

	public static PrintWriter openFileWrite(final File file)
	{
		PrintWriter outputFile;
		try
		{
			outputFile = new PrintWriter(new FileWriter(file));
		}
		catch (IOException e)
		{
			System.out.println("TextFileAccess.openFileWrite(): " + e);
			outputFile = null;
		}
		return (outputFile);
	}
	
	public static PrintWriter openFileWrite(final String fileName)
	{
		PrintWriter outputFile;
		try
		{
			outputFile = new PrintWriter(new FileWriter(fileName));
		}
		catch (IOException e)
		{
			System.out.println("TextFileAccess.openFileWrite(): " + e);
			outputFile = null;
		}
		return (outputFile);
	}
	
	public static PrintWriter openFileWriteEx(final File file) throws IOException
	{
		return new PrintWriter(new FileWriter(file));
	}

	public static BufferedReader openFileReadEx(final File file) throws IOException
	{
		return new BufferedReader(new FileReader(file));
	}

	public static PrintWriter openFileWriteEx(final String fileName) throws IOException
	{
		return new PrintWriter(new FileWriter(fileName));
	}

	public static BufferedReader openFileReadEx(final String fileName) throws IOException
	{
		return new BufferedReader(new FileReader(fileName));
	}
}
