# DDPoker Installer Build Strategy

## Problem Solved: No External Dependencies!

Your concerns about Install4j were valid:
- ❌ URL could change and break builds
- ❌ External dependency on ej-technologies.com
- ❌ Adds ~150MB to image
- ❌ Licensing/distribution complexity

## Solution: Use JDK's Built-in `jpackage`

**Java 14+ includes `jpackage`** which creates native installers without external tools!

### Advantages
- ✅ **No external dependencies** - included in JDK
- ✅ **No downloads** - always available
- ✅ **No licensing concerns** - part of OpenJDK
- ✅ **Creates native installers** - Windows .exe, Mac .dmg, Linux .deb/.rpm
- ✅ **Can embed JRE** - same as Install4j
- ✅ **Faster builds** - no compilation overhead
- ✅ **150MB smaller image** - no Install4j download

### What Gets Built

1. **Cross-platform JAR** (primary distribution)
   - ~20MB fat JAR
   - Requires Java 25 on user's machine
   - `java -jar DDPoker.jar`
   - **Build time: 1-2 seconds**

2. **Native app image** (optional, if jpackage works)
   - Platform-specific installer
   - Embeds JRE (no Java needed on user's machine)
   - Larger download (~150MB with embedded JRE)
   - **Build time: 5-10 seconds**

### How It Works

```bash
# Step 1: Copy pre-built classes (done at Docker build time)
COPY code/poker/target/classes/ /app/client-build/

# Step 2: At runtime, modify config files
sed -i "s|SERVER_HOST|your-server.com|g" config/poker/client.properties

# Step 3: Package into JAR (1-2 seconds)
jar cfm DDPoker.jar MANIFEST.MF -C . .

# Step 4: Optionally create native installer with jpackage (5-10 seconds)
jpackage --input . --name DDPoker --main-jar DDPoker.jar --type app-image
```

### For Your Use Case (20-50 friends)

**Recommended: Just use the JAR!**

Most of your friends probably have Java installed, or can easily install it. The JAR is:
- ✅ Small download (20MB vs 150MB)
- ✅ Cross-platform (works on Windows/Mac/Linux)
- ✅ Fast to build (1-2 seconds)
- ✅ No external dependencies

**If you want native installers:**
- jpackage works for Linux .deb packages (in the Docker container)
- For Windows .exe, you'd need to build on Windows (or use Wine)
- For Mac .dmg, you'd need to build on Mac

### Current Status

- ✅ Install4j removed from Dockerfile
- ✅ Image size reduced by ~150MB
- ✅ No external download dependencies
- ✅ jpackage support added (optional)
- ✅ JAR generation: 1-2 seconds
- ✅ No licensing/URL concerns

### Docker Commands

```bash
# Build the optimized image
docker build -f Dockerfile.optimized -t ddpoker:optimized .

# Run with your server hostname
SERVER_HOST=poker.yourserver.com docker run -p 8080:8080 ddpoker:optimized

# Downloads available at:
# http://poker.yourserver.com:8080/downloads/DDPoker.jar
```

### Recommendation

For your group of 20-50 friends:
1. **Provide the JAR** - simple, small, cross-platform
2. **Provide Java install instructions** - most will already have it
3. **Skip native installers** - not worth the complexity for small groups

This approach is simpler, more reliable, and has no external dependencies that could break!
