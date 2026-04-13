package ru.totemguard.geometry;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

/**
 * Represents an axis-aligned cuboid (rectangular prism) region in the world.
 * All coordinates are stored as absolute block positions.
 * Invariant: min <= max for all axes.
 */
public class CuboidRegion {
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    /**
     * Creates a cuboid region from two corner points.
     * The order of points does not matter — min/max are normalized.
     */
    public CuboidRegion(Vec3i pos1, Vec3i pos2) {
        this.minX = Math.min(pos1.getX(), pos2.getX());
        this.minY = Math.min(pos1.getY(), pos2.getY());
        this.minZ = Math.min(pos1.getZ(), pos2.getZ());
        this.maxX = Math.max(pos1.getX(), pos2.getX());
        this.maxY = Math.max(pos1.getY(), pos2.getY());
        this.maxZ = Math.max(pos1.getZ(), pos2.getZ());
    }

    /**
     * Creates a cuboid region from min/max bounds directly.
     * Assumes caller guarantees min <= max.
     */
    public CuboidRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * Creates a cuboid region centered on a position with given radius.
     * Height is 256 (full world height range for 1.20.1: -64 to 319).
     */
    public static CuboidRegion fromCenter(BlockPos center, int radius) {
        return new CuboidRegion(
                center.getX() - radius, -64, center.getZ() - radius,
                center.getX() + radius, 319, center.getZ() + radius
        );
    }

    /**
     * Creates a cuboid region from a vanilla BlockBox.
     */
    public static CuboidRegion fromBlockBox(BlockBox box) {
        return new CuboidRegion(
                box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ()
        );
    }

    // === Getters ===

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    /**
     * Returns the width of the region on the X axis.
     */
    public int getWidthX() {
        return maxX - minX + 1;
    }

    /**
     * Returns the depth of the region on the Z axis.
     */
    public int getDepthZ() {
        return maxZ - minZ + 1;
    }

    /**
     * Returns the center position of the region on the XZ plane.
     */
    public BlockPos getCenterXZ() {
        return new BlockPos((minX + maxX) / 2, 0, (minZ + maxZ) / 2);
    }

    /**
     * Converts this region to a vanilla BlockBox.
     */
    public BlockBox toBlockBox() {
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Checks if a block position is inside this region (inclusive boundaries).
     */
    public boolean contains(BlockPos pos) {
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    /**
     * Checks if this region intersects with another region.
     * Two regions intersect if they overlap on all three axes.
     */
    public boolean intersects(CuboidRegion other) {
        return this.minX <= other.maxX && this.maxX >= other.minX
                && this.minY <= other.maxY && this.maxY >= other.minY
                && this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    /**
     * Returns the smallest cuboid that contains both this region and the other region.
     * If they do not intersect, this still produces a valid bounding box.
     */
    public CuboidRegion union(CuboidRegion other) {
        return new CuboidRegion(
                Math.min(this.minX, other.minX),
                Math.min(this.minY, other.minY),
                Math.min(this.minZ, other.minZ),
                Math.max(this.maxX, other.maxX),
                Math.max(this.maxY, other.maxY),
                Math.max(this.maxZ, other.maxZ)
        );
    }

    /**
     * Returns a new region that is this region expanded by the given amount in all directions.
     */
    public CuboidRegion expand(int amount) {
        return new CuboidRegion(
                minX - amount, minY - amount, minZ - amount,
                maxX + amount, maxY + amount, maxZ + amount
        );
    }

    /**
     * Checks if this region equals another based on exact bounds.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CuboidRegion other)) return false;
        return minX == other.minX && minY == other.minY && minZ == other.minZ
                && maxX == other.maxX && maxY == other.maxY && maxZ == other.maxZ;
    }

    @Override
    public int hashCode() {
        int result = minX;
        result = 31 * result + minY;
        result = 31 * result + minZ;
        result = 31 * result + maxX;
        result = 31 * result + maxY;
        result = 31 * result + maxZ;
        return result;
    }

    @Override
    public String toString() {
        return String.format("CuboidRegion[(%d,%d,%d) -> (%d,%d,%d)]",
                minX, minY, minZ, maxX, maxY, maxZ);
    }
}
