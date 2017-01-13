import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

public class ImageProcessing {
	public static final ExecutorService parallel = Executors.newFixedThreadPool(8);
	public static void close(){
		parallel.shutdown();
	}
	public static enum Location{
		TOP(true), BOTTOM(true), LEFT(false), RIGHT(false);
		private boolean useWidth;
		Location(boolean useWidth)
		{
			this.useWidth = useWidth;
		}
		
		public int size(BufferedImage banner)
		{
			if (useWidth)
				return banner.getWidth();
			return banner.getHeight();
		}

		public boolean isInBanner(int x, int y, int size, BufferedImage banner) {
			switch(this)
			{
			case BOTTOM:
				return y >= size - banner.getHeight();
			case TOP:
				return y < banner.getHeight();
			case LEFT: 
				return x < banner.getWidth();
			case RIGHT: 
				return x >= size - banner.getWidth();
			}
			return false;
		}

		public int getRGB(int x, int y, int size, BufferedImage banner) {
			switch(this)
			{
			case BOTTOM:
				return banner.getRGB(x, y - (size - banner.getHeight()));
			case TOP:
				return banner.getRGB(x, y);
			case LEFT:
				return banner.getRGB(x, y);
			case RIGHT:
				return banner.getRGB(x - (size - banner.getWidth()), y);
			}
			// Returns something with opacity 0, so it shouldn't show up.
			return 0x00000000;
		}
	}

public static void main(String[] args) throws Throwable
  { 
	    double blend = 0.5;
	    boolean gray = true;
	    double brightness = .2;
	    double contrast = .2;
	    double gamma = 2;
	  
//    BufferedImage filter = javax.imageio.ImageIO.read(new File("/Users/Evan/Downloads/12571282_10209204612616311_552736146_n.png"));
//
//    BufferedImage collage = createCollageForDemo(filter, gray, blend, brightness, contrast, gamma);
//    
//    ImageIO.write(collage, "PNG", new File("collage.png"));
	    BufferedImage banner = javax.imageio.ImageIO.read(new File("lambdasaur_vertical.jpg"));
	    File[] files = new File("img/profiles").listFiles(new FilenameFilter(){

			@Override
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.endsWith(".jpg") || name.endsWith(".jpeg");
			}});
	    BufferedImage result = bannerImage(banner, javax.imageio.ImageIO.read(files[1]), Location.TOP);
	    ImageIO.write(result, "PNG", new File("bannerExample.png"));

    System.out.println("done");
    parallel.shutdownNow();
  }

   
  public static BufferedImage createCollageForDemo(BufferedImage filter, boolean gray,
		  double blend,
		  double brightness,
		  double contrast,
		  double gamma) throws Throwable
		  {
	  		File[] files = new File("img/profiles").listFiles(new FilenameFilter(){

				@Override
				public boolean accept(File dir, String name) {
					name = name.toLowerCase();
					return name.endsWith(".jpg") || name.endsWith(".jpeg");
				}});
	  		int size = Math.min(filter.getWidth(), filter.getHeight());
	  		List<BufferedImage> results = new ArrayList<>();
	  		
	  		//////////////////////////////////////
	  		// Doing extra work to allow for parallelization
	  		List<Callable<BufferedImage>> calls = new ArrayList<Callable<BufferedImage>>();
	  		for (final File file : files) {
	  			calls.add(new Callable<BufferedImage>(){
					@Override
					public BufferedImage call() throws Exception {
						BufferedImage img = javax.imageio.ImageIO.read(file);
					    BufferedImage result = filterImage(filter, img, gray, blend, brightness, contrast, gamma);
						return result;
					}});
	  		}
	  		
	  		List<Future<BufferedImage>> futures = parallel.invokeAll(calls);
	  		
	  		//////////////////////////////
	  		// End of parallel stuff, just get results
	  		for (Future<BufferedImage> future : futures)
	  		{
	  			BufferedImage result = future.get();
	  			results.add(result);
			    if (size > result.getHeight())
			    	size = result.getHeight();
	  		}
	  		
	  		
	  		
	  		// copies the test images into one big image.
	  		BufferedImage ret = new BufferedImage(3 * size, 2 * size, BufferedImage.TYPE_INT_ARGB);
	  		for (int i = 0; i < results.size(); i++)
	  		{
	  			BufferedImage img = results.get(i);
	  			if (img.getHeight() != size)
	  				img = resize(img, size);
	  			int startX = size * (i % 3);
			    int startY = size * (i / 3);
			    for (int x = 0; x < size; x++)
			    {
			    	for (int y = 0; y < size; y++)
			    	{
			    		ret.setRGB(x + startX, y + startY, img.getRGB(x, y));
			    	}
			    }
			    
	  		}
		    
		    return ret;
		  }
  
	
  // FIXME include a location parameter, but for now, assume BOTTOM
  // FIXME add more parameters?
  public static BufferedImage bannerImage(BufferedImage banner, BufferedImage img, Location loc)
  {
		// make only the image square
		if (img.getHeight() != img.getWidth())
		{
			int size = Math.min(img.getHeight(), img.getWidth());
			img = crop(img, size);
		}
		
		// go based on largest dimension of the banner??? 
		// FIXME should be related to the location parameter
		int size = Math.min(img.getHeight(), loc.size(banner));
		if (img.getHeight() != size)
			img = resize(img, size);
		else if (banner.getWidth() != size)
			// rescales with non-square images FIXME use `Location`
			banner = resize(banner, size, false);
		
		BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < size; x++)
		{
			for (int y = 0; y < size; y++)
			{
				result.setRGB(x, y, img.getRGB(x, y));
				
				if (loc.isInBanner(x, y, size, banner))
					result.setRGB(x, y, loc.getRGB(x, y, size, banner));
			}
		}
		return result;
  }

  // FIXME add more options?
  public static BufferedImage overlayImage(BufferedImage overlay,BufferedImage img)
		  {
	  if (overlay.getHeight() != overlay.getWidth())
	    {
	    	int size = Math.min(overlay.getHeight(), overlay.getWidth());
	    	overlay = crop(overlay, size);
	    }
	    // make square
	    if (img.getHeight() != img.getWidth())
	    {
	    	int size = Math.min(img.getHeight(), img.getWidth());
	    	img = crop(img, size);
	    }
	    
	    int size = Math.min(img.getHeight(), overlay.getHeight());
	    if (img.getHeight() != size)
	    	img = resize(img, size);
	    else if (overlay.getHeight() != size)
	    	overlay = resize(overlay, size);
	    
	    BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	    for (int x = 0; x < size; x++)
	    {
	    	for (int y = 0; y < size; y++)
	    	{
	    		int overlayAlpha = overlay.getRGB(x, y) & 0xff000000;
	    		if (overlayAlpha != 0)
	    			result.setRGB(x, y, overlay.getRGB(x,y));
	    		else
	    			result.setRGB(x, y, img.getRGB(x, y));
	    	}
	    }
	    return result;
		  }
  
  public static BufferedImage filterImage(BufferedImage filter,
		  BufferedImage img,
		  boolean gray,
		  double blend,
		  double brightness,
		  double contrast,
		  double gamma)
		  {
	    if (filter.getHeight() != filter.getWidth())
	    {
	    	int size = Math.min(filter.getHeight(), filter.getWidth());
	    	filter = crop(filter, size);
	    }
	    // make square
	    if (img.getHeight() != img.getWidth())
	    {
	    	int size = Math.min(img.getHeight(), img.getWidth());
	    	img = crop(img, size);
	    }
	    
	    int size = Math.min(img.getHeight(), filter.getHeight());
	    if (img.getHeight() != size)
	    	img = resize(img, size);
	    else if (filter.getHeight() != size)
	    	filter = resize(filter, size);
	    
	    if (gray) {
	    	img = gray(img);
	    }
	    
	    // at this point, dimensions for both are the same
	    BufferedImage result = blend(img, filter, blend, size);
	    
	    // FIXME add brightness, contrast, gamma corrections
	    result = adjust(result, brightness, contrast, gamma);
	    return result;
		  }
  
  private static BufferedImage adjust(BufferedImage result, double brightness,
		double contrast, double gamma) {
	  // contrast http://www.dfstudios.co.uk/articles/programming/image-programming-algorithms/image-processing-algorithms-part-5-contrast-adjustment/
	  // brightness http://math.stackexchange.com/questions/906240/algorithms-to-increase-or-decrease-the-contrast-of-an-image
	  
	  for (int x = 0; x < result.getWidth(); x++)
		  for (int y = 0; y < result.getHeight(); y++)
		  {
			  int orig = result.getRGB(x, y);
			  int a = orig & 0xff000000;
			  int r = (orig & 0x00ff0000) >>> 16;
	  		int g = (orig & 0x0000ff00) >>> 8;
	  		int b = (orig & 0x000000ff);
	  		r = update(r, brightness, contrast, gamma);
	  		g = update(g, brightness, contrast, gamma);
	  		b = update(b, brightness, contrast, gamma);
	  		int newRGB = a + (r << 16) + (g << 8) + b;
	  		result.setRGB(x, y, newRGB);
		  }
	return result;
}

private static int update(int pixel, double brightness, double contrast,
		double gamma) {
	double contrastFactor = -((259 * (1 + contrast))/(-259 + 255 * contrast));
	double updated = contrastFactor * (pixel - 128) + 128 + 128 * brightness;
	pixel = correctRange(updated);
	// pixel now in range [0, 0xff].
	pixel = correctRange(0xff * Math.pow(pixel / 255.0, gamma));
	
	return pixel;
}
private static int correctRange(double pixelVal)
{
	return Math.min(Math.max((int)Math.round(pixelVal),0), 0xff);
}

private static BufferedImage resize(BufferedImage img, int size)
  {
	return resize(img, size, true);
  }
private static BufferedImage resize(BufferedImage img, int size, boolean isSquare)
{
	if (isSquare) {
	  //http://stackoverflow.com/a/9417836
		Image tmp = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
	    BufferedImage dimg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();
	    return dimg;
	}
	
	// Assume that size is intended to be the larger dimension
	int newWidth = size;
	int newHeight = size;
	if (img.getWidth() < img.getHeight())
	{
		// height expands to `size`
		newWidth = (int)Math.round(img.getWidth() * (size + 0.0) / img.getHeight()); 
	}
	else
	{
		newHeight = (int)Math.round(img.getHeight() * (size + 0.0) / img.getWidth()); 
	}
	Image tmp = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    BufferedImage dimg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

    Graphics2D g2d = dimg.createGraphics();
    g2d.drawImage(tmp, 0, 0, null);
    g2d.dispose();
    return dimg;
}
  private static BufferedImage blend(BufferedImage img, BufferedImage filter,
		double blend, int size) {
	  BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	    for (int x = 0; x < size; x++)
	    {
	    	for (int y = 0; y < size; y++)
	    	{
	    		int imgrgb = img.getRGB(x, y);
	    		int filterrgb = filter.getRGB(x, y);
	    		
	    		int imga = (imgrgb & 0xff000000) >>> 24;
	    		int imgr = (imgrgb & 0x00ff0000) >>> 16;
	    		int imgg = (imgrgb & 0x0000ff00) >>> 8;
	    		int imgb = imgrgb & 0x000000ff;
	    		
	    		int fila = (filterrgb & 0xff000000) >>> 24;
	    		int filr = (filterrgb & 0x00ff0000) >>> 16;
	    		int filg = (filterrgb & 0x0000ff00) >>> 8;
	    		int filb = filterrgb & 0x000000ff;
	    		
	    		int resa = correctRange(blend * fila + (1-blend) * imga);
	    		int resr = correctRange(blend * filr + (1-blend) * imgr);
	    		int resg = correctRange(blend * filg + (1-blend) * imgg);
	    		int resb = correctRange(blend * filb + (1-blend) * imgb);
	    		
	    		int resrgb = (resa << 24) + (resr << 16) + (resg << 8) + resb;
	    		result.setRGB(x, y, resrgb);
	    	}
	    }
	return result;
}

  public static BufferedImage crop(BufferedImage img, int size)
  {
	BufferedImage ret = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	int startX = (img.getWidth() - size) / 2;
	int startY = (img.getHeight() - size) / 2;
	for (int x = 0; x < size; x++)
		for (int y = 0; y < size; y++)
		{
			int rgb = img.getRGB(x + startX,y + startY);
			ret.setRGB(x, y, rgb);
		}
	  
	  return ret;
  }
  public static BufferedImage gray(BufferedImage img)
  {
	  int size = img.getWidth();
	  BufferedImage ret = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	  for (int x = 0; x < size; x++)
      {
      	for (int y = 0; y < size; y++)
      	{
      		int imgrgb = img.getRGB(x, y);
      		int a = (imgrgb & 0xff000000);
      		int r = (imgrgb & 0x00ff0000) >>> 16;
      		int g = (imgrgb & 0x0000ff00) >>> 8;
      		int b = (imgrgb & 0x000000ff);
      		int grayLevel = correctRange((r + g + b)/3.0);
      		imgrgb = a + (grayLevel << 16) + (grayLevel << 8) + grayLevel;
      		ret.setRGB(x, y, imgrgb);
      	}
      }
	  return ret;
  }
}
