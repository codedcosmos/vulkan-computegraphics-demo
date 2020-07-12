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

public class ResourceUtils {
	public static float[] getCubeVertices() {
		float[] vertices = new float[] {
				-1f, -1f, -1f,
				 1f, -1f, -1f,
				 1f,  1f, -1f,
				-1f,  1f, -1f,
				-1f, -1f,  1f,
				 1f, -1f,  1f,
				 1f,  1f,  1f,
				-1f,  1f,  1f,
		};
		return vertices;
	}
	
	public static int[] getCubeIndices() {
		int[] indices = new int[] {
				0, 1, 3, 3, 1, 2,
				1, 5, 2, 2, 5, 6,
				5, 4, 6, 6, 4, 7,
				3, 0, 7, 7, 0, 3,
				3, 2, 7, 7, 2, 6,
				4, 5, 0, 0, 5, 1,
		};
		return indices;
	}
	
	public static float[] getVertices() {
		float[] vertices = new float[] {
				-0.5f, -0.5f,  0.5f,
				1.0f, 0.0f, 0.0f,
				0.5f, -0.5f, 0.5f,
				0.0f, 1.0f, 0.0f,
				0.0f, 0.5f, 0.5f,
				0.0f, 0.0f, 1.0f,
				0.5f, -0.5f, -0.5f,
				1.0f, 1.0f, 0.0f,
				-0.5f, -0.5f, -0.5f,
				0.0f, 1.0f, 1.0f,
				0.0f, 0.5f, -0.5f,
				1.0f, 0.0f, 1.0f};
		return vertices;
	}
}
