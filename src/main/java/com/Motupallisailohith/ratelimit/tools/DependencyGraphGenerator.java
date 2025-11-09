package com.Motupallisailohith.ratelimit.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates a DOT format dependency graph for the entire codebase
 */
public class DependencyGraphGenerator {
    
    private static final String BASE_PACKAGE = "com.Motupallisailohith.ratelimit";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([a-zA-Z0-9_.]+);");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(public\\s+|private\\s+|protected\\s+)?(static\\s+)?(final\\s+)?(abstract\\s+)?(class|interface|enum)\\s+(\\w+)");
    
    private Map<String, ClassInfo> classes = new HashMap<>();
    
    static class ClassInfo {
        String packageName;
        String className;
        String fullName;
        Set<String> imports = new HashSet<>();
        Set<String> dependencies = new HashSet<>();
        String type = "class"; // class, interface, or enum
        
        ClassInfo(String packageName, String className, String type) {
            this.packageName = packageName;
            this.className = className;
            this.fullName = packageName + "." + className;
            this.type = type;
        }
    }
    
    public void analyzeSourceDirectory(Path sourceDir) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(this::analyzeJavaFile);
        }
    }
    
    private void analyzeJavaFile(Path javaFile) {
        try {
            List<String> lines = Files.readAllLines(javaFile);
            String packageName = null;
            String className = null;
            String classType = "class";
            Set<String> imports = new HashSet<>();
            
            for (String line : lines) {
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
                    continue;
                }
                
                // Extract package
                Matcher packageMatcher = PACKAGE_PATTERN.matcher(line);
                if (packageMatcher.find()) {
                    packageName = packageMatcher.group(1);
                    continue;
                }
                
                // Extract imports
                Matcher importMatcher = IMPORT_PATTERN.matcher(line);
                if (importMatcher.find()) {
                    String importedClass = importMatcher.group(1);
                    // Only track our own package imports
                    if (importedClass.startsWith(BASE_PACKAGE)) {
                        imports.add(importedClass);
                    }
                    continue;
                }
                
                // Extract class/interface/enum name
                if (className == null) {
                    Matcher classMatcher = CLASS_PATTERN.matcher(line);
                    if (classMatcher.find()) {
                        classType = classMatcher.group(5); // Group 5 is class/interface/enum
                        className = classMatcher.group(6); // Group 6 is the name
                    }
                }
            }
            
            if (packageName != null && className != null) {
                ClassInfo info = new ClassInfo(packageName, className, classType);
                info.imports.addAll(imports);
                classes.put(info.fullName, info);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + javaFile + " - " + e.getMessage());
        }
    }
    
    private void resolveDependencies() {
        // For each class, resolve which specific classes it depends on
        for (ClassInfo classInfo : classes.values()) {
            for (String importedClass : classInfo.imports) {
                // Check if this is a class in our codebase
                if (classes.containsKey(importedClass)) {
                    classInfo.dependencies.add(importedClass);
                } else {
                    // It might be a wildcard or nested class, try to match
                    for (String knownClass : classes.keySet()) {
                        if (knownClass.startsWith(importedClass)) {
                            classInfo.dependencies.add(knownClass);
                        }
                    }
                }
            }
        }
    }
    
    public String generateDotGraph() {
        resolveDependencies();
        
        StringBuilder dot = new StringBuilder();
        dot.append("digraph DependencyGraph {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=box, style=filled, fontname=\"Arial\"];\n");
        dot.append("  edge [color=\"#666666\", arrowsize=0.8];\n\n");
        
        // Group classes by package
        Map<String, List<ClassInfo>> byPackage = classes.values().stream()
            .collect(Collectors.groupingBy(c -> c.packageName));
        
        // Define colors for different packages and types
        Map<String, String> packageColors = new HashMap<>();
        packageColors.put(BASE_PACKAGE, "#E8F5E9"); // Main - Light green
        packageColors.put(BASE_PACKAGE + ".bucket", "#E3F2FD"); // Bucket - Light blue
        packageColors.put(BASE_PACKAGE + ".protocol", "#FFF3E0"); // Protocol - Light orange
        packageColors.put(BASE_PACKAGE + ".reliability", "#F3E5F5"); // Reliability - Light purple
        packageColors.put(BASE_PACKAGE + ".security", "#FFEBEE"); // Security - Light red
        packageColors.put(BASE_PACKAGE + ".server", "#FFF9C4"); // Server - Light yellow
        packageColors.put(BASE_PACKAGE + ".tools", "#E0F2F1"); // Tools - Light teal
        
        // Create subgraphs for each package
        int clusterIndex = 0;
        for (Map.Entry<String, List<ClassInfo>> entry : byPackage.entrySet()) {
            String packageName = entry.getKey();
            List<ClassInfo> classList = entry.getValue();
            
            String color = packageColors.getOrDefault(packageName, "#F5F5F5");
            String shortPackageName = packageName.replace(BASE_PACKAGE + ".", "").replace(BASE_PACKAGE, "main");
            
            dot.append(String.format("  subgraph cluster_%d {\n", clusterIndex++));
            dot.append(String.format("    label=\"%s\";\n", shortPackageName));
            dot.append("    style=filled;\n");
            dot.append(String.format("    color=\"%s\";\n", color));
            dot.append("    fontsize=12;\n");
            dot.append("    fontname=\"Arial Bold\";\n\n");
            
            // Add nodes for each class in this package
            for (ClassInfo classInfo : classList) {
                String nodeId = classInfo.fullName.replace(".", "_");
                String nodeColor = getNodeColor(classInfo.type);
                String shape = getNodeShape(classInfo.type);
                
                dot.append(String.format("    %s [label=\"%s\", fillcolor=\"%s\", shape=%s];\n", 
                    nodeId, classInfo.className, nodeColor, shape));
            }
            
            dot.append("  }\n\n");
        }
        
        // Add edges (dependencies)
        dot.append("  // Dependencies\n");
        for (ClassInfo classInfo : classes.values()) {
            String fromNode = classInfo.fullName.replace(".", "_");
            for (String dependency : classInfo.dependencies) {
                String toNode = dependency.replace(".", "_");
                dot.append(String.format("  %s -> %s;\n", fromNode, toNode));
            }
        }
        
        // Add legend
        dot.append("\n  // Legend\n");
        dot.append("  subgraph cluster_legend {\n");
        dot.append("    label=\"Legend\";\n");
        dot.append("    style=filled;\n");
        dot.append("    color=\"#FFFFFF\";\n");
        dot.append("    fontsize=10;\n");
        dot.append("    node [fontsize=10];\n");
        dot.append("    legend_class [label=\"Class\", fillcolor=\"#B3E5FC\", shape=box];\n");
        dot.append("    legend_interface [label=\"Interface\", fillcolor=\"#C8E6C9\", shape=diamond];\n");
        dot.append("    legend_enum [label=\"Enum\", fillcolor=\"#FFE0B2\", shape=ellipse];\n");
        dot.append("  }\n");
        
        dot.append("}\n");
        
        return dot.toString();
    }
    
    private String getNodeColor(String type) {
        switch (type) {
            case "interface":
                return "#C8E6C9"; // Light green for interfaces
            case "enum":
                return "#FFE0B2"; // Light orange for enums
            default:
                return "#B3E5FC"; // Light blue for classes
        }
    }
    
    private String getNodeShape(String type) {
        switch (type) {
            case "interface":
                return "diamond";
            case "enum":
                return "ellipse";
            default:
                return "box";
        }
    }
    
    public static void main(String[] args) {
        try {
            String baseDir = args.length > 0 ? args[0] : ".";
            String outputFile = args.length > 1 ? args[1] : "dependency-graph.dot";
            
            Path sourceDir = Paths.get(baseDir, "src", "main", "java");
            
            if (!Files.exists(sourceDir)) {
                System.err.println("Source directory not found: " + sourceDir);
                System.exit(1);
            }
            
            DependencyGraphGenerator generator = new DependencyGraphGenerator();
            generator.analyzeSourceDirectory(sourceDir);
            
            String dotGraph = generator.generateDotGraph();
            
            Path outputPath = Paths.get(outputFile);
            Files.write(outputPath, dotGraph.getBytes());
            
            System.out.println("Dependency graph generated successfully!");
            System.out.println("Output file: " + outputPath.toAbsolutePath());
            System.out.println("Total classes analyzed: " + generator.classes.size());
            System.out.println("\nTo visualize the graph, use one of the following:");
            System.out.println("  1. Online: https://dreampuf.github.io/GraphvizOnline/");
            System.out.println("  2. Command line (if graphviz installed): dot -Tpng " + outputFile + " -o dependency-graph.png");
            System.out.println("  3. Command line (SVG): dot -Tsvg " + outputFile + " -o dependency-graph.svg");
            
        } catch (Exception e) {
            System.err.println("Error generating dependency graph: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
