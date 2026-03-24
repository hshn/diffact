# Changelog

## [0.7.0](https://github.com/hshn/diffact/compare/v0.6.0...v0.7.0) (2026-03-24)


### Features

* replace sync extension methods with a reusable Sync builder ([#42](https://github.com/hshn/diffact/issues/42)) ([0018f2c](https://github.com/hshn/diffact/commit/0018f2cc0ec9aceb5870fb652b508ccb0126b839))

## [0.6.0](https://github.com/hshn/diffact/compare/v0.5.0...v0.6.0) (2026-03-10)


### Features

* add type parameter to OptionDiffer for type-safe DiffResult ([#39](https://github.com/hshn/diffact/issues/39)) ([25af3d0](https://github.com/hshn/diffact/commit/25af3d09ef4d9290aab37fd8dc341662b7e5113c))

## [0.5.0](https://github.com/hshn/diffact/compare/v0.4.0...v0.5.0) (2026-03-09)


### Features

* add Scala 3.3 LTS support ([#37](https://github.com/hshn/diffact/issues/37)) ([6300149](https://github.com/hshn/diffact/commit/63001497f99f17e560933253f5e9b696bc0e8f1a))

## [0.4.0](https://github.com/hshn/diffact/compare/v0.3.0...v0.4.0) (2026-03-08)


### Features

* improve API consistency across Differ types ([#34](https://github.com/hshn/diffact/issues/34)) ([4dd67dc](https://github.com/hshn/diffact/commit/4dd67dc6cf560afb3a0979eb4c3f1184a0a85a88))

## [0.3.0](https://github.com/hshn/diffact/compare/v0.2.0...v0.3.0) (2026-03-08)


### Features

* generalize OptionDiffer to accept any Differ[A] ([#32](https://github.com/hshn/diffact/issues/32)) ([f825f3b](https://github.com/hshn/diffact/commit/f825f3b06f01b07bbb402cafda27cf2e5b306043))

## [0.2.0](https://github.com/hshn/diffact/compare/v0.1.0...v0.2.0) (2026-02-23)


### Features

* add EitherDBIOComponent for Either-aware DBIO operations ([#10](https://github.com/hshn/diffact/issues/10)) ([8c67e3a](https://github.com/hshn/diffact/commit/8c67e3ac4f4a28aa43dee3ceb0dfb9215d34a9c8))
* add generic fold method to Difference ([#27](https://github.com/hshn/diffact/issues/27)) ([159e86c](https://github.com/hshn/diffact/commit/159e86c0b282c663a4200cf75f3f3e8fbdabed72))
* add Show / pretty-print support for Difference ([#28](https://github.com/hshn/diffact/issues/28)) ([0dcc2ff](https://github.com/hshn/diffact/commit/0dcc2ff8ead4174974ef6852726b8802d23d8189))
* preserve key information in MapDiffer diff results ([#16](https://github.com/hshn/diffact/issues/16)) ([#29](https://github.com/hshn/diffact/issues/29)) ([7781495](https://github.com/hshn/diffact/commit/7781495005d4e7f078026f33c515ff4db4d7f355))


### Bug Fixes

* make SeqDiffer.diff result ordering deterministic ([#17](https://github.com/hshn/diffact/issues/17)) ([#25](https://github.com/hshn/diffact/issues/25)) ([3235546](https://github.com/hshn/diffact/commit/3235546fb788d5ab745bf028af5ca52b8eadba3e))

## 0.1.0 (2026-02-21)


### Features

* add diffact library with core, slick, and zio modules ([a2d4055](https://github.com/hshn/diffact/commit/a2d4055bfee42245827830761a040f8013b1e1f5))
