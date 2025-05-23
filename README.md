# Digital Derivans

![JDK11 Maven3](https://github.com/ulb-sachsen-anhalt/digital-derivans/workflows/Java%20CI%20with%20Maven/badge.svg)

Java command line tool to create PDF files from image derivates with configurable scales, qualities and additional assets files like preview images or thumbnails.  
Appends image footer or structured OCR formats (ALTO, PAGE) to produce text layers and metadata (METS/MODS).

Please note: Derivans does no metadata validation or sanitizing.  
If provided METS/MODS, it fails for empty logical sections, i.e. chapters not linked to any image or page because it can't create a consistent outline in this case. To ensure XML conformance before/after using Derivans, one needs to use a different tool.

* [Features](#features)
* [Local Installation](#installation)
* [Usage](#usage)
* [Configuration](#configuration)
* [Limitations](#limitations)
* [License](#license)

## Features

Create PDF from scaled image data (optional: footer) and constraints on compression rate and max sizes.  
For details see [configuration section](#configuration).

If metadata (METS/MODS) available, the following will be taken into account:

* Value `mods:recordIdentifier[@source]` to name PDF artefact
  Can be replaced in PDF-step configuration with option `mods_identifier_xpath` or at execution time via CLI-parameter `-n` / `--name-pdf`
* Value `mods:titleInfo/mods:title` for internal naming
* Attribute `mets:div[@CONTENTIDS]` (granular URN) will be rendered for each page if footer shall be appended to each page image

## Docker Image

Pull the [Docker image](https://github.com/ulb-sachsen-anhalt/digital-derivans/pkgs/container/digital-derivansout):

```bash
docker pull ghcr.io/ulb-sachsen-anhalt/digital-derivans:latest
```

or build it your own locally:

```bash
./scripts/build_docker_image.sh
```

Usage of docker image is described in [Usage](#usage) section, but all required directories / files need to be passed as mapped volumes.

For example:

```bash
docker run \
  --mount type=bind,source=<host-work-dir>,target=/data-print \
  --mount type=bind,source=<host-config-dir>,target=/data-config \
  --mount type=bind,source=<host-log-dir>,target=/data-log \
  ghcr.io/ulb-sachsen-anhalt/digital-derivans \ 
  <print-dir|mets-file> -c /data-config/derivans.ini  
```

## Local Installation

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

This will create a shaded JAR ("FAT-JAR") inside the build directory (`./target/digital-derivans-<version>.jar`)

## Usage

In local mode, a recent OpenJRE is required.

The tool expects a project folder containing an image directory (default: `DEFAULT`) and optional OCR-data directory (
default: `FULLTEXT`').

The *default name* of the generated PDF inside is derived from the object's folder name or can be set with `-n`-arg.

A sample folder structure:

```bash
my_print/
├── FULLTEXT
│   ├── 0002.xml
│   ├── 0021.xml
│   ├── 0332.xml
├── DEFAULT
│   ├── 0002.tif
│   ├── 0021.tif
│   ├── 0332.tif
```

Running

```bash
java -jar <PATH>./target/digital-derivans-<version>.jar <path-to-my_print>`
```

will produce a file named `my_print.pdf` in the `my_print` directory from above with specified layout.  
For more information concerning CLI-Usage, please [consult CLI docs](#cli-parameter).

## Configuration

Although Derivans can be run without configuration, it's strongly recommended. Many flags, especially if metadata must
be taken into account, are using defaults tied to digitization workflows of ULB Sachsen-Anhalt that *might* not fit
your custom requirements.

### Configure Sections

Configuration options can be bundled into sections and customized with a INI-file.

Some params can be set on global level, like quality and poolsize.  
Each section in a `*.ini`- file matching `[derivate_<n>]` represents a single derivate section for intermediate or final
derivates.

Order of execution is determined by numbering of derivate sections at parse-time.

### Default Values

On top of the INI-file are global configuration options listed, which will be used as defaults for actual steps, if they can be
applied.

* `default_quality`  : image data compression rate (can be specified with `quality` for image derivate sections)
* `default_poolsize` : poolsize of worker threads for parallel processing (can be specified with `poolsize` for image
  derivate sections)

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

### Minimal working Example

The following example configuration contains global settings and subsequent generation steps.  
(Example directory and file layout like from [Usage section](#usage) assumed.)

On global level, it sets the default JPEG-quality to `75`, the number of parallel executors to `4` (recommended if at
least 4 CPUs available) and determines the file for the logging-configuration.

1. Create JPEG images from images in sub directory `MAX` with compression rate 75, scale to maximal dimension 1000px and store
   in sub dir `IMAGE_75`.
2. Create PDF with images from `IMAGE_75`, add some PDF metadata and store file as `my_print.pdf` in current dir.

```ini
default_quality = 75
default_poolsize = 4
logger_configuration_file = derivans_logging.xml

[derivate_01]
input_dir = MAX
output_dir = IMAGE_75
maximal = 1000

[derivate_02]
type = pdf
input_dir = IMAGE_75
output_dir = .
output_type = pdf
metadata_creator = "<your organization label>"
metadata_license = "<your licence>"
```

### CLI Parameter

The main parameter for Derivans is the input path, which may be a local directory in local mode or the path to a local
METS/MODS-file with sub directories for images and OCR-data, if using metadata.

Additionally, one can also provide via CLI

* `-c` path to custom configuration INI-file
* `-d` flag to turn on rendering of boxes and text if using OCR input
* `-n` set custom name for resulting PDF
* set labels for OCR and input-image (will overwrite configuration)  
  If metadata present, both will be used as filegroup names;
  For images they will also be used as input directory for initial image processing

## Limitations

Derivans depends on standard JDK11-components and external components for image processing and PDF generation.

### Step Configuration

* Subsequent derivate steps must not have order gaps, since the parsing is done step by step. Otherwise, any derivate
  section after the first gap will be ignored, which may lead to unexpected results.

### Image Processing

*Please note*:  
To overcome `javax.imageio` errors, it's recommended to fix them using an external image processing application.

* Images with more than 8bit channel depth can't be processed  [javax.imageio.IIOException: Illegal band size](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/42)
* Uncommon image metadata can't be processed  
[javax.imageio.IIOException: Unsupported marker](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/33)
* Integral dimension values required for proper scaling  [javax.imageio.metadata.IIOInvalidTreeException: Xdensity attribute out of range](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/53)

### PDF Generation

* If Derivans is called from within the project folder, the resulting pdf will be called `..pdf`.
* iText PDF-Library limits the maximal page dimension to 14400 px (
  weight/height, [Configured max dimension fails for very large Images](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/16)).
  This may cause trouble if one needs to generate PDF for very large prints like maps, deeds or scrolls.

### Metadata

* Derivans [does not accept METS with current OCR-D-style](https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/38)
nor any other METS which contains extended XML-features like inline namespace declarations.

## License

This project's source code is licensed under terms of the [MIT license](https://opensource.org/licenses/MIT).

NOTE: This project depends on components that may use different license terms.
