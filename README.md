# Digital Derivans

![JDK11 Maven3](https://github.com/ulb-sachsen-anhalt/digital-derivans/workflows/Java%20CI%20with%20Maven/badge.svg) 

Derive new digitals from existing ones.

Uses [mets-model](https://github.com/MyCoRe-Org/mets-model) for METS/MODS-handling, classical [iText5](https://github.com/itext/itextpdf) to create PDF, [Apache log4j2](https://github.com/apache/logging-log4j2) for logging and a workflow inspired by [OCR-D/Core](https://github.com/OCR-D/core) METS-driven-Workflows.

* [Features](#features)
* [Installation](#installation)
* [Usage](#usage)
* [Configuration](#configuration)
* [License](#license)

## Features

Create JPG or PDF from TIF or JPG with optional Footer appended and custom constraints on compression rate and max sizes. For details see [configuration section](#Configuration).

If METS/MODS-information is avaiable, the following will be utilized:

* `mods:recordInfo/mods:recordIdentifier` to name the resulting PDF-file
* for PDF-outline attribute `mets:div[@ORDER]` for the file containers is strictly respected as defined in the METS physical structMap

If METS/MODS is available and a footer with proper labelling is required, then additionally the following will be utilized:

* `mods:identifier[@type="urn"]` as default URN in Image Footer
to name the PDF-file
* if granular URNs exist in METS physical strucMap, information from `mets:div[@CONTENTIDS]` will be used for each individual page in Image Footer

## Installation

Digital Derivans is a Java project build with [Apache Maven](https://github.com/apache/maven).

### Requirements

* OpenJDK 11+
* Maven 3.6+
* git 2.12+
* build application

  ```shell
  mvn clean package
  ```

  This will create a shaded JAR ("FAT-JAR") which can be passed to the actual target system. _Please note, that an recent OpenJDK is required to run the Application._

## Usage

Call via Cmdline: `java -jar <application-jarfile>.jar <path-to-data>`

## Configuration

Several options can be customized with a plain INI-file.
In subdir `config` next to derivans JAR place the following files:

* config/derivans.ini
* config/derivans_logging.xml
* config/footer_template.png

Even `derivans.ini` is not mandatory. This scenario is only usable for evaluation purposes, since several output parameters can only be guessed by the system or remain unset (aka `n.a.`).

### Configure Sections

Each section in `derivans.ini`- file represents a single derivate section, used to derive Images or PDFs.
Additionally, there can be a global section which sets default values for all subsequent steps.
Order of Workflow is determined by pairs of input-output paths, whereas numbering of derivate sections determines order at parse-time.

### Default Values

On top of the INI-file are configuration values listed, which will be used as defaults for actual steps, if they can be applied.

* `default_quality`  : image data compression rate (can be specified with `quality` for image derivate sections)
* `default_poolsize` : poolsize of worker threads for parallel processing (can be specified with `poolsize` for image derivate sections)

### Section-specific Configuration

Some options values must be set individually for each step:

* `input_dir` : path to directory with section input files
* `output_dir`: path to directory for section output

Additional options can be set, according to of the actual type to derive:

* `quality` : compression rate (JPG)
* `poolsize` : parallel workers (JPG)
* `maximal` : maximal width/height (JPG)
* `footer_template` : generic footer from template with URN from METS/MODS (JPG)
* `footer_label_copyright` : additional static label for footer (JPG)
* `metadata_creator` : enrich creator tag (PDF)
* `metadata_keywords`: enrich keywords (PDF)

### Example

The following example configuration contains global settings and 3 subsequent steps.
On global level, it set the default JPG-quality to `80`, the number of parallel executors to `8` and determines the file for the logging-configuration.

1. Create new JPGs from directory `MAX` with compression rate 95 and footer data and store them in dir `IMAGE_FOOTER`.
2. Create new JPGs from directory `IMAGE_FOOTER` with compression rate 80, max dimension 1000 and store them in dir `IMAGE_80`.
3. Create new PDF with images from `IMAGE_80`, add some metadata and store in current dir.

```ini
default_quality = 80
default_poolsize = 8
logger_configuration_file = derivans_logging.xml

[derivate_01]
quality = 95
input_dir = MAX
output_dir = IMAGE_FOOTER
# path to footer_template relative to dir "config"
footer_template = footer_template.png
footer_label_copyright = "Universitäts- und Landesbibliothek Sachsen-Anhalt"

[derivate_02]
input_dir = IMAGE_FOOTER
output_dir = IMAGE_80
maximal = 1000

[derivate_03]
type = pdf
input_dir = IMAGE_80
output_dir = .
output_type = pdf
metadata_creator = "Universitäts- und Landesbibliothek Sachsen-Anhalt"
metadata_keywords = "VD18,Retro-Digitalisierung"
```

## License

Licensed under terms of the [GNU Affero General Public License v3.0](https://spdx.org/licenses/AGPL-3.0-or-later.html).
