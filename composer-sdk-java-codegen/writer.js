/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

class Writer {

    /**
     * Create a Writer.
     *
     */
    constructor() {
        this.beforeBuffer = '';
        this.buffer = '';
        this.linesWritten = 0;
    }

    /**
     * Writes text to the start of the buffer
     * @param {int} tabs - the number of tabs to use
     * @param {string} text - the text to write
     */
    writeBeforeLine(tabs,text) {
        for(let n=0; n < tabs; n++) {
            this.beforeBuffer += '   ';
        }
        this.beforeBuffer += text;
        this.beforeBuffer += '\n';
        this.linesWritten++;
    }

    /**
     * Append text to the buffer
     * @param {int} tabs - the number of tabs to use
     * @param {string} text - the text to write
     */
    writeLine(tabs,text) {
        for(let n=0; n < tabs; n++) {
            this.write('\t');
        }
        this.write(text);
        this.write('\n');
        this.linesWritten++;
    }

    /**
     * Returns the number of lines that have been written to the buffer.
     * @return {int} the number of lines written to the buffer.
     */
    getLineCount() {
        return this.linesWritten;
    }


    /**
     * Append text to the buffer, prepending tabs
     * @param {int} tabs - the number of tabs to use
     * @param {string} text - the text to write
     */
    writeIndented(tabs,text) {
        for(let n=0; n < tabs; n++) {
            this.write('   ');
        }
        this.write(text);
    }

    /**
     * Append text to the buffer (no automatic newline). The
     * text may contain newline, and these will increment the linesWritten
     * counter.
     * @param {string} msg - the text to write
     */
    write(msg) {
        if(typeof msg !== 'string' ) {
            throw new Error('Can only append strings. Argument ' + msg + ' has type ' + typeof msg);
        }

        this.buffer += msg;
        this.linesWritten += msg.split(/\r\n|\r|\n/).length;
    }

    /**
     * Returns the text that has been buffered in this Writer.
     * @return {string} the buffered text.
     */
    getBuffer() {
        return this.beforeBuffer + this.buffer;
    }

    /**
     * Empties the underyling buffer and resets the line count.
     */
    clearBuffer() {
        this.beforeBuffer = '';
        this.buffer = '';
        this.linesWritten = 0;
    }
}

module.exports = Writer;
