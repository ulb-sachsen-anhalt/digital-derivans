# Digital Derivans

![JDK11 Maven3](https://github.com/ulb-sachsen-anhalt/digital-derivans/workflows/Java%20CI%20with%20Maven/badge.svg)

Java command line tool that creates image derivates with different, configurable sizes and qualities, appends additional image footer and may assemble image files and OCR data to produce searchable pdf files with hidden text layer and an outline.

Uses [mets-model](https://github.com/MyCoRe-Org/mets-model) for METS/MODS-handling, classical [iText5](https://github.com/itext/itextpdf) to create PDF, [Apache log4j2](https://github.com/apache/logging-log4j2) for logging and a workflow inspired by [OCR-D/Core](https://github.com/OCR-D/core) Workflows.

* [Features](#features)
* [Local Installation](#installation)
* [Usage](#usage)
* [Configuration](#configuration)
* [Limitations](#limitations)
* [License](#license)

## Features

Create JPG or PDF from TIF or JPG with optional Footer appended and custom constraints on compression rate and max sizes. For details see [configuration section](#configuration).

If METS/MODS-information is available, the following will be taken into account:

* Attribute `mets:div[@ORDER]` for file containers as defined in the METS physical structMap forms the PDF outline
* Attribute `mets:div[@CONTENTIDS]` (granular URN) will be used for each individual page if image footer will be generated

## Installation

Digital Derivans is a Java 11+ project build with [Apache Maven](https://github.com/apache/maven).

### Development Requirements

* OpenJDK 11+
* Maven 3.6+
* git 2.12+

### Pull and compile

Clone the repository and call Maven to trigger the build process, but be aware, that a recent OpenJDK is required.

```shell
git clone git@github.com:ulb-sachsen-anhalt/digital-derivans.git
cd digital-derivans
mvn clean package
```

This will first run the tests and afterwards create a shaded JAR ("FAT-JAR") inside the build directory (`./target/digital-derivans-<version>.jar`)

## Usage

In local mode, a recent OpenJRE is required.

The tool expects a project folder containing an image directory (default: `MAX`) and optional OCR-data directory (default: `FULLTEXT`').

The default name of the generated pdf is derived from the project folder name.

A sample folder structure:

```bash
test/
├── FULLTEXT
│   ├── 0002.xml
│   ├── 0021.xml
│   ├── 0332.xml
├── MAX
│   ├── 0002.tif
│   ├── 0021.tif
│   ├── 0332.tif
```

Running

```bash
java -jar <PATH>./target/digital-derivans-<version>.jar test/`
```

will produce a pdf `test.pdf` in the `test/` directory from above with specified layout.  
For more information concerning CLI-Usage, [please see](#cli-parameter).

## Configuration

Although Derivans can be run without separate configuration file, it's strongly recommended because many flags, especially if metadata must be taken into account, are using defaults tied to digitalization workflows of ULB Sachsen-Anhalt that *might* not fit your custom requirements.

### Configure Sections

Configuration options can be bundled into sections and customized with a INI-file.

Some params can be set on global level in section `[]` . Each section in a `*.ini`- file matching `[derivate_<n>]` represents a single derivate section for intermediate or resulting derivates.

Order of execution is determined by pairs of input-output paths, whereas numbering of derivate sections determines order at parse-time.

### Default Values

On top of the INI-file are configuration values listed, which will be used as defaults for actual steps, if they can be applied.

* `default_quality`  : image data compression rate (can be specified with `quality` for image derivate sections)
* `default_poolsize` : poolsize of worker threads for parallel processing (can be specified with `poolsize` for image derivate sections)

### Section-specific Configuration

Some options values must be set individually for each step:

* `input_dir` : path to directory with section input files
* `output_dir`: path to directory for section output

Additional options can be set, according to of the actual type to derive:

Images:

* `quality` : compression rate
* `poolsize` : parallel workers
* `maximal` : maximal dimension (affects both width and height)
* `footer_template` : footer template Path
* `footer_label_copyright` : additional (static) label for footer

PDF:

* `metadata_creator` : enrich creator tag
* `metadata_keywords`: enrich keywords
* `enrich_pdf_metadata` : if PDF shall be enriched into METS/MODS (default: `True`)
* `mods_identifier_xpath` : if not set, use `mods:recordIdentifier` from primary MODS
* `mets_filegroup_fulltext`: METS-filegroup for OCR-Data (default: `FULLTEXT`)
* `mets_filegroup_images` : METS-filegroup for image data (default: `MAX`)

### Minimal working Example

The following example configuration contains global settings and 3 subsequent steps.
On global level, it sets the default JPG-quality to `75`, the number of parallel executors to `4` (recommended if at least 4 CPUs available) and determines the file for the logging-configuration.

1. Create new JPGs from project workdir subdirectory `DEFAULT` with compression rate 95 and footer data and store them in dir `IMAGE_FOOTER`.
2. Create new JPGs from directory `IMAGE_FOOTER` with compression rate 75, max dimension 1000 and store them in dir `IMAGE_75`.
3. Create new PDF with images from `IMAGE_75`, add some metadata and store in current dir.

```ini
default_quality = 75
default_poolsize = 4
logger_configuration_file = derivans_logging.xml

[derivate_01]
input_dir = DEFAULT
output_dir = IMAGE_75
maximal = 1000

[derivate_03]
type = pdf
input_dir = IMAGE_75
output_dir = .
output_type = pdf
metadata_creator = "<your organization label>"
metadata_license = "Public Domain Mark 1.0"
```

### CLI Parameter

The main parameter for Derivans is the input path, which may be a local directory in local mode or the path to a local METS/MODS-file with sub directories for images and OCR-data, if using metadata.

Additionally, one can also provide via CLI

* path to custom configuration INI-file
* set labels for OCR and input-image (will overwrite configuration)  
  If metadata present, both will be used as filegroup names;
  For images they will also be used as input directory for inital image processing

## Limitations

Derivans depends on standard JDK-components and external Libraries for image processing and PDF generation.

### Image Processing

* OpenJRE/OpenJDK can't process image data with more than 8bit channel depth ([javax.imageio.IIOException: Illegal band size](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/42)). To overcome this, one needs to reduce channel depth with an external tool.
* Corrupt or exotic image metadata leads to process errors, since the metadata is copied too, when generating image derivates for proper scaling of PDF ([javax.imageio.IIOException: Unsupported marker](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/33)).

### PDF Generation

* If Derivans is called from within the project folder, the resulting pdf will be called `..pdf`.
* PDF-Library limits the maximal dimension to 14400 px (weight/height, [Configured max dimension fails for very large Images](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/16)). This may cause trouble if one needs to generate PDF for very large prints like maps, deeds or scrolls.

### Metadata

* Derivans [does not accept METS with OCR-D-style](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/38) if it contains extended XML-features like inline namespace declarations.

## License

Licensed under terms of the [GNU Affero General Public License v3.0](https://spdx.org/licenses/AGPL-3.0-or-later.html).
