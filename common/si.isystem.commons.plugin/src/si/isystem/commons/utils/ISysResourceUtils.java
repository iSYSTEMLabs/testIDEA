package si.isystem.commons.utils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

public class ISysResourceUtils {

    private static ImageDescriptor MISSING_IMG_DESCRIPTOR = null;
    
	private static final Map<String, ImageDescriptor> s_imageDescriptors = new HashMap<>();
	private static final Map<String, Image> s_images = new HashMap<>();

	public static String getResourceFromBundle(String resourcePath,
			String bundleName) throws IOException {
		Bundle bundle = Platform.getBundle(bundleName);
		URL entryUrl = bundle.getEntry(resourcePath);
		URL resourceUrl = org.eclipse.core.runtime.FileLocator.toFileURL(entryUrl);
		return FileLocator.resolve(resourceUrl).getFile();
	}

	public static ImageDescriptor getImageDescriptor(String resourcePath, String bundleName) {
		String key = bundleName + "///" + resourcePath;

		if (!s_imageDescriptors.containsKey(key)) {
			ImageDescriptor imgDescr = AbstractUIPlugin.imageDescriptorFromPlugin(bundleName, resourcePath);
			if (imgDescr == null) {
			    imgDescr = getMissingImagePlaceholderImage();
			}
			s_imageDescriptors.put(key, imgDescr);
		}

		return s_imageDescriptors.get(key);
	}

	private static ImageDescriptor getMissingImagePlaceholderImage() {
	    if (MISSING_IMG_DESCRIPTOR == null) {
	        Image img = new Image(Display.getDefault(), new Rectangle(0, 0, 16, 16));
	        MISSING_IMG_DESCRIPTOR = ImageDescriptor.createFromImageData(img.getImageData());
	    }
	    return MISSING_IMG_DESCRIPTOR;
    }

    public static Image getImage(String resourcePath, String bundleName) {
		String key = bundleName + "///" + resourcePath;
		if (!s_images.containsKey(key)) {
			ImageDescriptor imgDescr = getImageDescriptor(resourcePath, bundleName);
			Image img = imgDescr.createImage();
			s_images.put(key, img);
		}

		return s_images.get(key);
	}
}
