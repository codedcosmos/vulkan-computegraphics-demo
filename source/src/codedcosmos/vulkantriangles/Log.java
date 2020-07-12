/*
 *     VulkanTriangles by codedcosmos
 *
 *     VulkanTriangles is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License 3 as published by
 *     the Free Software Foundation.
 *     VulkanTriangles is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License 3 for more details.
 *     You should have received a copy of the GNU General Public License 3
 *     along with VulkanTriangles.  If not, see <https://www.gnu.org/licenses/>.
 */

package codedcosmos.vulkantriangles;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Log {
	// Print
	public static void print(Object... line) {
		String completedLine = getFormatted(line);
		
		System.out.println(completedLine);
	}
	
	public static void printErr(Object... line) {
		String completedLine = getFormatted(line);
		
		System.err.println(completedLine);
	}
	
	// Format
	public static String getFormatted(Object... line) {
		String text = "";
		
		for (Object t : line) {
			text += getFormatted(t) + " ";
		}
		
		return text;
	}
	
	public static String getFormatted(Object object) {
		if (object instanceof Throwable) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			((Throwable)object).printStackTrace(pw);
			return sw.toString();
		}
		
		//
		
		return object.toString();
	}
}
