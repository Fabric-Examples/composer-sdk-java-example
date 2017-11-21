/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const cc = require('composer-common');

const BusinessNetworkDefinition = cc.BusinessNetworkDefinition;
const ModelManager = cc.ModelManager;
const ModelUtil = cc.ModelUtil;
const ModelFile = cc.ModelFile;
const ClassDeclaration = cc.ClassDeclaration;
const Field = cc.Field;
const RelationshipDeclaration = cc.RelationshipDeclaration;
const EnumDeclaration = cc.EnumDeclaration;
const EnumValueDeclaration = cc.EnumValueDeclaration;
const FunctionDeclaration = cc.FunctionDeclaration;
const FileWriter = require("./filewriter");
const fs = require("fs");
const AnnotationPrefix = 'org.hyperledger.composer.annotation';
const EscapeTypes = ["Transaction", "Asset", "Participant"];

/**
 * Convert the contents of a BusinessNetworkDefinition to Java code.
 * Set a fileWriter property (instance of FileWriter) on the parameters
 * object to control where the generated code is written to disk.
 *
 * @private
 * @class
 * @memberof module:composer-common
 */
class JavaVisitor {


	/**
	 * parse system cto and output java file into given dir
	 * @param {string} dir - output dir path string
	 * @param {string} input - input bna path (optional)
	 */
	visitAsync(dir, input) {
		let option = {fileWriter: new FileWriter(dir)};
		if (input) {
			option.ignoreSystem = true;
			return BusinessNetworkDefinition.fromArchive(fs.readFileSync(input))
				.then((bnd) => {
					this.visit(bnd, option);
				});
		} else {
			return Promise.resolve().then(() => {
				this.visit(new ModelManager(), option);
			});
		}
	}

	/**
	 * Visitor design pattern
	 * @param {Object} thing - the object being visited
	 * @param {Object} parameters  - the parameter
	 * @return {Object} the result of visiting or null
	 * @private
	 */
	visit(thing, parameters) {
		if (thing instanceof BusinessNetworkDefinition) {
			return this.visitBusinessNetworkDefinition(thing, parameters);
		} else if (thing instanceof ModelManager) {
			return this.visitModelManager(thing, parameters);
		} else if (thing instanceof ModelFile) {
			return this.visitModelFile(thing, parameters);
		} else if (thing instanceof EnumDeclaration) {
			return this.visitEnumDeclaration(thing, parameters);
		} else if (thing instanceof ClassDeclaration) {
			return this.visitClassDeclaration(thing, parameters);
		} else if (thing instanceof Field) {
			return this.visitField(thing, parameters);
		} else if (thing instanceof RelationshipDeclaration) {
			return this.visitRelationship(thing, parameters);
		} else if (thing instanceof EnumValueDeclaration) {
			return this.visitEnumValueDeclaration(thing, parameters);
		} else if (thing instanceof FunctionDeclaration) {
			// return this.visitEnum(thing, parameters);
		} else {
			throw new Error('Unrecognised type: ' + typeof thing + ', value: ' + thing);
		}
	}

	/**
	 * Visitor design pattern
	 * @param {BusinessNetworkDefinition} businessNetworkDefinition - the object being visited
	 * @param {Object} parameters  - the parameter
	 * @return {Object} the result of visiting or null
	 * @private
	 */
	visitBusinessNetworkDefinition(businessNetworkDefinition, parameters) {
		businessNetworkDefinition.getModelManager().accept(this, parameters);
		return null;
	}

	/**
	 * Visitor design pattern
	 * @param {ModelManager} modelManager - the object being visited
	 * @param {Object} parameters  - the parameter
	 * @return {Object} the result of visiting or null
	 * @private
	 */
	visitModelManager(modelManager, parameters) {
		modelManager.getModelFiles().forEach((modelFile) => {
			modelFile.accept(this, parameters);
		});
		return null;
	}

	/**
	 * Visitor design pattern
	 * @param {ModelFile} modelFile - the object being visited
	 * @param {Object} parameters  - the parameter
	 * @return {Object} the result of visiting or null
	 * @private
	 */
	visitModelFile(modelFile, parameters) {

		modelFile.getAllDeclarations().forEach((decl) => {
			decl.accept(this, parameters);
		});

		return null;
	}

	/**
	 * Write a Java class file header. The class file will be created in
	 * a file/folder based on the namespace of the class.
	 * @param {ClassDeclaration} clazz - the clazz being visited
	 * @param {Object} parameters  - the parameter
	 * @private
	 */
	startClassFile(clazz, parameters) {
		console.log("generating " + clazz.getName() + ".java");
		parameters.fileWriter.openFile(clazz.getModelFile().getNamespace().replace(/\./g, '/') + '/' + clazz.getName() + '.java');
		parameters.fileWriter.writeLine(0, '/*');
        parameters.fileWriter.writeLine(0, ' * Copyright IBM Corp. 2017 All Rights Reserved.');
        parameters.fileWriter.writeLine(0, ' *');
        parameters.fileWriter.writeLine(0, ' * SPDX-License-Identifier: Apache-2.0');
        parameters.fileWriter.writeLine(0, ' */')
        parameters.fileWriter.writeLine(0, '')
		parameters.fileWriter.writeLine(0, '// this code is generated and should not be modified');
		parameters.fileWriter.writeLine(0, 'package ' + clazz.getModelFile().getNamespace() + ';');
		parameters.fileWriter.writeLine(0, '');
	}

	/**
	 * Close a Java class file
	 * @param {ClassDeclaration} clazz - the clazz being visited
	 * @param {Object} parameters  - the parameter
	 * @private
	 */
	endClassFile(clazz, parameters) {
		parameters.fileWriter.closeFile();
	}


	/**
	 * Visitor design pattern
	 * @param {EnumDeclaration} enumDeclaration - the object being visited
	 * @param {Object} parameters  - the parameter
	 * @return {Object} the result of visiting or null
	 * @private
	 */
	visitEnumDeclaration(enumDeclaration, parameters) {
		if (this.shouldIgnore(enumDeclaration, parameters)) return null;
		this.startClassFile(enumDeclaration, parameters);

		parameters.fileWriter.writeLine(0, `@${AnnotationPrefix}.Enum`);
		parameters.fileWriter.writeLine(0, 'public enum ' + enumDeclaration.getName() + ' {');

		enumDeclaration.getOwnProperties().forEach((property) => {
			property.accept(this, parameters);
		});

		parameters.fileWriter.writeLine(0, '}');

		this.endClassFile(enumDeclaration, parameters);

		return null;
	}

	/**
	 * Visitor design pattern
	 * @param {ClassDeclaration} classDeclaration - the object being visited
	 * @param {Object} parameters  - the parameter
	 * @return {Object} the result of visiting or null
	 * @private
	 */
	visitClassDeclaration(classDeclaration, parameters) {
		if (this.shouldIgnore(classDeclaration, parameters) || this.escapeType(classDeclaration.name) === 'Object') return null;
		this.startClassFile(classDeclaration, parameters);

		classDeclaration.getModelFile().getImports().forEach((imported) => {
			if (imported.indexOf(ModelUtil.getSystemNamespace()) < 0) {
				parameters.fileWriter.writeLine(0, 'import ' + imported + ';');
			}
		});

		let isAbstract = classDeclaration.isAbstract() ? 'abstract ' : '';
		let superType = '';
		if (classDeclaration.getSuperType()) {
			superType = ' extends ' + this.escapeType(classDeclaration.getSuperType());
		}

		parameters.fileWriter.writeLine(0, `@${AnnotationPrefix}.` + classDeclaration.constructor.name.replace('Declaration', ''));
		parameters.fileWriter.writeLine(0, 'public ' + isAbstract + 'class ' + classDeclaration.getName() + superType + ' {');

		const primaryKeyName = classDeclaration.getIdentifierFieldName();
		classDeclaration.getOwnProperties().forEach((property) => {
			if (property.name === primaryKeyName) {
				parameters.isPrimary = true;
			}
			property.accept(this, parameters);
			parameters.isPrimary = false;
		});

		parameters.fileWriter.writeLine(0, '}');
		this.endClassFile(classDeclaration, parameters);

		return null;
	}

	/**
	 *
	 * @param classDeclaration
	 * @param parameter
	 */
	shouldIgnore(classDeclaration, parameter) {
		return parameter.ignoreSystem && classDeclaration.isSystemType();
	}

	/**
	 *
	 * @param {string} type
	 */
	escapeType(type) {
		return EscapeTypes.indexOf(type) < 0 && EscapeTypes.indexOf(ModelUtil.getShortName(type)) < 0 ? type : 'Object';
	}

	/**
	 * Visitor design pattern
	 * @param {Field} field - the object being visited
	 * @param {Object} parameters  - the parameter
	 * @return {Object} the result of visiting or null
	 * @private
	 */
	visitField(field, parameters) {
		let array = '';

		if (field.isArray()) {
			array = '[]';
		}
		parameters.fileWriter.writeLine(1, `@${AnnotationPrefix}.DataField(primary=${parameters.isPrimary}, optional=${field.isOptional()}, embedded=true)`);
		parameters.fileWriter.writeLine(1, 'public ' + this.toJavaType(field.getType()) + array + ' ' + field.getName() + ';');
		return null;
	}

	/**
	 * Visitor design pattern
	 * @param {EnumValueDeclaration} enumValueDeclaration - the object being visited
	 * @param {Object} parameters  - the parameter
	 * @return {Object} the result of visiting or null
	 * @private
	 */
	visitEnumValueDeclaration(enumValueDeclaration, parameters) {
		parameters.fileWriter.writeLine(1, enumValueDeclaration.getName() + ',');
		return null;
	}

	/**
	 * Visitor design pattern
	 * @param {Relationship} relationship - the object being visited
	 * @param {Object} parameters  - the parameter
	 * @return {Object} the result of visiting or null
	 * @private
	 */
	visitRelationship(relationship, parameters) {
		let array = relationship.isArray() ? '[]' : '';
		// we export all relationships by capitalizing them
		parameters.fileWriter.writeLine(1, `@${AnnotationPrefix}.Pointer(optional=${relationship.isOptional()})`);
		parameters.fileWriter.writeLine(1, 'public ' + this.toJavaType(relationship.getType()) + array + ' ' + relationship.getName() + ';');
		return null;
	}

	/**
	 * Converts a Composer type to a Java type. Primitive types are converted
	 * everything else is passed through unchanged.
	 * @param {string} type  - the composer type
	 * @return {string} the corresponding type in Java
	 * @private
	 */
	toJavaType(type) {
		switch (type) {
			case 'DateTime':
				return 'java.util.Date';
			case 'Boolean':
				return 'boolean';
			case 'String':
				return 'String';
			case 'Double':
				return 'double';
			case 'Long':
				return 'long';
			case 'Integer':
				return 'int';
			default:
				return this.escapeType(type);
		}
	}
}

module.exports = JavaVisitor;
