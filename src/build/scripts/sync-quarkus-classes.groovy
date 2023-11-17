/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the 'License'); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Get AppendBuffer and ResteasyReactiveOutputStream from Quarkus and adapt them for Quarkus CXF
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

new Transform()

public class Transform {

    public Transform() throws IOException {
        final Path destinationDir = Paths.get('src/main/java/io/quarkiverse/cxf/transport/generated');

        JavaParser parser = new JavaParser(StaticJavaParser.getParserConfiguration());
        final CompilationUnit appendBuffer = parse('org/jboss/resteasy/reactive/server/vertx/AppendBuffer.java', parser);
        String quarkusVersion = 'main';
        transformCommon(appendBuffer, quarkusVersion);
        store(appendBuffer, destinationDir);

        final CompilationUnit resteasyReactiveOutputStream = parse(
                'org/jboss/resteasy/reactive/server/vertx/ResteasyReactiveOutputStream.java', parser);
        transformCommon(resteasyReactiveOutputStream, quarkusVersion);
        transformStream(resteasyReactiveOutputStream, parser);
        store(resteasyReactiveOutputStream, destinationDir);
    }

    private void transformCommon(final CompilationUnit unit, String quarkusVersion) {
        TypeDeclaration<?> primaryType = unit.getType(0);
        String cmt = 'Adapted by sync-quarkus-classes.groovy from\n' +
                '<a href=\n' +
                '\'https://github.com/quarkusio/quarkus/blob/' + quarkusVersion +
                '/independent-projects/resteasy-reactive/server/vertx/src/main/java/org/jboss/resteasy/reactive/server/vertx/ResteasyReactiveOutputStream.java\'><code>ResteasyReactiveOutputStream</code></a>\n' +
                'from Quarkus.\n';
        Optional<JavadocComment> javaDoc = primaryType.getJavadocComment();
        if (javaDoc.isEmpty()) {
            JavadocComment newJavaDoc = new JavadocComment(cmt);
            primaryType.setComment(newJavaDoc);
        } else {
            javaDoc.get().setContent(cmt + '\n<p>\n' + javaDoc.get().getContent());
        }
        unit.getPackageDeclaration().get().setName('io.quarkiverse.cxf.transport.generated');

    }

    private void transformStream(final CompilationUnit unit, JavaParser parser) {

        /* Remove some imports */
        NodeList<ImportDeclaration> imports = unit.getImports();
        for (Iterator<ImportDeclaration> i = imports.iterator(); i.hasNext();) {
            ImportDeclaration imp = i.next();
            String cl = imp.getNameAsString();
            if (cl.startsWith('jakarta.ws.rs.')
                    || cl.startsWith('org.jboss.resteasy.')
                    || cl.contains('java.io.OutputStream')) {
                i.remove();
            }
        }

        Stream.of(
                'io.quarkus.vertx.core.runtime.VertxBufferImpl',
                'io.quarkiverse.cxf.transport.VertxReactiveRequestContext',
                'jakarta.servlet.ServletOutputStream',
                'jakarta.servlet.WriteListener',
                'io.vertx.core.http.HttpServerResponse')
                .map(cl -> new ImportDeclaration(cl, false, false))
                .forEach(imports::add);
        final String[] prefixOrder = new String[]{ 'java.', 'jakarta.', 'org.' };
        Comparator<ImportDeclaration> importComparator = new Comparator<ImportDeclaration>() {
            @Override
            public int compare(ImportDeclaration i1, ImportDeclaration i2) {
                String s1 = i1.getNameAsString();
                String s2 = i2.getNameAsString();
                // Define the order of prefixes

                // Get the index of the prefixes in the array or -1 if not found
                int indexS1 = -1;
                int indexS2 = -1;
                for (int i = 0; i < prefixOrder.length; i++) {
                    if (s1.startsWith(prefixOrder[i])) {
                        indexS1 = i;
                        break;
                    }
                }
                for (int i = 0; i < prefixOrder.length; i++) {
                    if (s2.startsWith(prefixOrder[i])) {
                        indexS2 = i;
                        break;
                    }
                }

                // Compare based on prefix order
                if (indexS1 != -1 && indexS2 != -1) {
                    return Integer.compare(indexS1, indexS2); // Compare based on prefix order
                } else if (indexS1 != -1) {
                    return -1; // s1 starts with a known prefix, so it comes first
                } else if (indexS2 != -1) {
                    return 1; // s2 starts with a known prefix, so it comes first
                } else {
                    return s1.compareTo(s2); // Compare alphabetically if none of the prefixes match
                }
            }
        };
        Collections.sort(imports, importComparator);

        /* Rename to VertxServletOutputStream */
        TypeDeclaration<?> primaryType = unit.getType(0);
        primaryType.setName('VertxServletOutputStream');
        for (ConstructorDeclaration constructor : primaryType.getConstructors()) {
            constructor.setName('VertxServletOutputStream');
            constructor.getParameter(0).setType('VertxReactiveRequestContext');
        }
        /* Change the supertype */
        primaryType.asClassOrInterfaceDeclaration().getExtendedTypes(0).setName('ServletOutputStream');

        /* Use our fake context */
        primaryType.getFieldByName('context').get().getVariable(0).setType('VertxReactiveRequestContext');

        /* Remove contentLengthSet() */
        primaryType.getMethodsByName('contentLengthSet').forEach(m -> m.remove());

        /* Remove ContentLengthSetResult */
        primaryType.getMembers().stream()
            .filter(m -> m.isEnumDeclaration())
            .map(m -> (EnumDeclaration) m)
            .filter(m -> m.getNameAsString().equals('ContentLengthSetResult'))
            .collect(Collectors.toList()).stream()
            .forEach(m -> m.remove());

        /* Replace prepareWrite() */
        final String body =
                '    private void prepareWrite(ByteBuf buffer, boolean finished) throws IOException {\n' +
                '        if (!committed) {\n' +
                '            committed = true;\n' +
                '            if (finished) {\n' +
                '                final HttpServerResponse response = request.response();\n' +
                '                if (!response.headWritten()) {\n' +
                '                    if (buffer == null) {\n' +
                '                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, \"0\");\n' +
                '                    } else {\n' +
                '                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(buffer.readableBytes()));\n' +
                '                    }\n' +
                '                }\n' +
                '            } else {\n' +
                '                request.response().setChunked(true);\n' +
                '            }\n' +
                '        }\n' +
                '    }\n';
        final MethodDeclaration newPrepareWrite = parser.parseMethodDeclaration(body).getResult().get();
        primaryType.getMethodsByName('prepareWrite').forEach(m -> m.setBody(newPrepareWrite.getBody().get()));


        /* Add @Override where needed */
        addOverride(primaryType, 'close', 'flush', 'write');

        /* Make ServletOutputStream happy */
        MethodDeclaration isReady = primaryType.addMethod('isReady', Keyword.PUBLIC);
        isReady.addAnnotation('Override');
        isReady.setType('boolean');
        BlockStmt isReadyBody = new BlockStmt();
        isReadyBody.addStatement(new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(null, 'UnsupportedOperationException'), new NodeList<>())));
        isReady.setBody(isReadyBody);


        MethodDeclaration setWriteListener = primaryType.addMethod('setWriteListener', Keyword.PUBLIC);
        setWriteListener.addAnnotation(Override.class);
        // Add the writeListener parameter
        setWriteListener.addParameter(new Parameter(parser.parseClassOrInterfaceType('WriteListener').getResult().get(), 'writeListener'));
        BlockStmt setWriteListenerBody = new BlockStmt();
        setWriteListenerBody.addStatement(new ThrowStmt(new ObjectCreationExpr(null, new ClassOrInterfaceType(null, 'UnsupportedOperationException'), new NodeList<>())));
        setWriteListener.setBody(setWriteListenerBody);

    }

    private void addOverride(TypeDeclaration<?> primaryType, String... methodNames) {
        Stream.of(methodNames)
                .map(methodName -> primaryType.getMethodsByName(methodName))
                .flatMap(List::stream)
                .filter(methodDeclaration -> !methodDeclaration.getNameAsString().equals('write')
                        && methodDeclaration.getParameters().size() > 0
                        && !methodDeclaration.getParameter(0).getTypeAsString().equals('ByteBuf'))
                .forEach(methodDeclaration -> methodDeclaration.addAnnotation('Override'));
    }

    private void store(CompilationUnit unit, Path destinationDir) throws IOException {
        String name = unit.getType(0).getNameAsString();
        Path file = destinationDir.resolve(name + '.java');
        final String oldContent = Files.exists(file) ? Files.readString(file) : null;
        final String newContent = unit.toString().replace('@Override()', '@Override');
        if (!newContent.equals(oldContent)) {
            println('Updating ' + name + '.java')
            Files.createDirectories(destinationDir);
            Files.write(file, newContent.getBytes(StandardCharsets.UTF_8));
        } else {
            println(name + '.java up to date')
        }
    }

    CompilationUnit parse(String resourcePath, JavaParser parser) throws IOException {
        final String src = new String(getClass().getClassLoader().getResourceAsStream(resourcePath).readAllBytes(),
                StandardCharsets.UTF_8);
        return parser.parse(src).getResult().get();
    }

}